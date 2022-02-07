package ninjaphenix.gradle.mod;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.spongepowered.gradle.vanilla.MinecraftExtension;
import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Acknowledgements:
//  Common source set inspired by https://github.com/samolego/MultiLoaderTemplate
public final class GradlePlugin implements Plugin<Project> {
    private boolean validatedVanillaGradleVersion = false;
    private boolean validatedLoomVersion = false;
    private boolean validatedForgeGradleVersion = false;

    @Override
    public void apply(Project target) {
        this.validateGradleVersion(target);

        Task buildTask = target.task("buildMod");

        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                project.apply(Map.of("plugin", "java-library"));
                project.setGroup("ninjaphenix");
                project.setVersion(templateProject.property("mod_version") + "+" + Constants.MINECRAFT_VERSION);
                project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(templateProject.<String>property("archives_base_name"));
                project.setBuildDir(project.getRootDir().toPath().resolve("build/" + project.getName()));

                project.getExtensions().configure(JavaPluginExtension.class, extension -> {
                    extension.setSourceCompatibility(Constants.JAVA_VERSION);
                    extension.setTargetCompatibility(Constants.JAVA_VERSION);
                });

                project.getTasks().withType(JavaCompile.class).configureEach(task -> task.getOptions().setEncoding("UTF-8"));

                project.getDependencies().add("implementation", "org.jetbrains:annotations:" + Constants.JETBRAINS_ANNOTATIONS_VERSION);

                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.getTasks().getByName("build"));
                }

               templateProject.getCommonProject().ifPresent(common -> {
                   project.getTasks().withType(ProcessResources.class).configureEach(task -> {
                       task.from(common.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main").getResources());
                   });

                   project.getTasks().withType(JavaCompile.class).configureEach(task -> {
                       task.source(common.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main").getAllSource());
                   });

                   project.getDependencies().add("compileOnly", common);
               });

                if (templateProject.getPlatform() == Platform.FABRIC) {
                    this.applyFabric(templateProject, target);
                } else if (templateProject.getPlatform() == Platform.COMMON) {
                    this.applyCommon(templateProject, target);
                }
            }
        });
    }

    private void applyCommon(TemplateProject templateProject, Project target) {
        this.validateVanillaGradleVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "org.spongepowered.gradle.vanilla"));
        project.getExtensions().configure(MinecraftExtension.class, extension -> {
            extension.version(Constants.MINECRAFT_VERSION);
        });
    }

    private void validateGradleVersion(Project target) {
        boolean isCorrectGradleVersion = target.getGradle().getGradleVersion().equals(Constants.REQUIRED_GRADLE_VERSION);
        List<String> tasks = target.getGradle().getStartParameter().getTaskNames();
        boolean isExecutingWrapperTaskOnly = tasks.size() == 1 && tasks.get(0).equals(":wrapper");
        if (!isCorrectGradleVersion && !isExecutingWrapperTaskOnly) {
            throw new IllegalStateException("This plugin requires gradle " + Constants.REQUIRED_GRADLE_VERSION + " to update run: ./gradlew :wrapper --gradle-version " +  Constants.REQUIRED_GRADLE_VERSION);
        }
    }

    private void applyFabric(TemplateProject templateProject, Project target) {
        this.validateLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "fabric-loom"));
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        if (templateProject.usesDataGen()) {
            sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/main/generated"));

            sourceSets.create("datagen", sourceSet -> {
                sourceSet.getCompileClasspath().plus(sourceSets.getByName("main").getCompileClasspath());
                sourceSet.getRuntimeClasspath().plus(sourceSets.getByName("main").getCompileClasspath());
                sourceSet.getCompileClasspath().plus(sourceSets.getByName("main").getOutput());
                sourceSet.getRuntimeClasspath().plus(sourceSets.getByName("main").getOutput());
            });
        }

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", project.getExtensions().getByType(LoomGradleExtensionAPI.class).officialMojangMappings());
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.property("fabric_loader_version"));
        if (project.hasProperty("fabric_api_version")) {
            dependencies.add("modImplementation", "net.fabricmc.fabric-api:fabric-api:" + templateProject.property("fabric_api_version"));
        }

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
                if (templateProject.usesDataGen()) {
                    container.create("datagen", settings -> {
                        settings.client();
                        settings.vmArg("-Dfabric-api.datagen");
                        // todo: needs checking.
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                        settings.runDir("build/fabric-datagen");
                        settings.source(sourceSets.getByName("datagen"));
                    });
                }
            });

            //noinspection UnstableApiUsage
            extension.getMixin().getUseLegacyMixinAp().set(false);

            if (project.hasProperty("access_widener_path")) {
                extension.getAccessWidenerPath().set(project.file(templateProject.property("access_widener_path")));
            }
        });

        //noinspection UnstableApiUsage
        project.getTasks().withType(ProcessResources.class).configureEach(task -> {
            HashMap<String, String> props = new HashMap<>();
            props.put("version", templateProject.property("mod_version"));
            task.getInputs().properties(props);
            task.filesMatching("fabric.mod.json", details -> details.expand(props));
            task.exclude(".cache/*");
        });
    }

    private void validateLoomVersionIfNeeded(Project target) {
        if (!validatedLoomVersion) {
            Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : artifacts) {
                ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
                if (identifier.getGroup().equals("net.fabricmc") && identifier.getName().equals("fabric-loom")) {
                    String loomVersion = identifier.getVersion();
                    if (!loomVersion.equals(Constants.REQUIRED_LOOM_VERSION)) {
                        throw new IllegalStateException("This plugin requires loom " + Constants.REQUIRED_LOOM_VERSION + ", current is " + loomVersion + ".");
                    } else {
                        validatedLoomVersion = true;
                        return;
                    }
                }
            }
            throw new IllegalStateException("This plugin requires loom, add it to the current project un-applied.");
        }
    }

    private void validateVanillaGradleVersionIfNeeded(Project target) {
        if (!validatedVanillaGradleVersion) {
            Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : artifacts) {
                ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
                if (identifier.getGroup().equals("org.spongepowered") && identifier.getName().equals("vanillagradle")) {
                    String vanillaGradleVersion = identifier.getVersion();
                    if (!vanillaGradleVersion.equals(Constants.REQUIRED_VANILLA_GRADLE_VERSION)) {
                        throw new IllegalStateException("This plugin requires vanilla gradle " + Constants.REQUIRED_VANILLA_GRADLE_VERSION + ", current is " + vanillaGradleVersion + ".");
                    } else {
                        validatedVanillaGradleVersion = true;
                        return;
                    }
                }
            }
            throw new IllegalStateException("This plugin requires loom, add it to the current project un-applied.");
        }
    }
}
