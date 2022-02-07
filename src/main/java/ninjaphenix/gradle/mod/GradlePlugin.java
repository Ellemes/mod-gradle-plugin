package ninjaphenix.gradle.mod;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.minecraftforge.gradle.userdev.UserDevExtension;
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
import org.spongepowered.asm.gradle.plugins.MixinExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Acknowledgements:
//  Common project inspired by https://github.com/samolego/MultiLoaderTemplate
public final class GradlePlugin implements Plugin<Project> {
    private boolean validatedLoomVersion = false;
    private boolean validatedForgeGradleVersion = false;
    private boolean validatedMixinGradleVersion = false;

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

                if (templateProject.usesDataGen()) {
                    SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
                    sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/main/generated"));

                    sourceSets.create("datagen", sourceSet -> {
                        sourceSet.getCompileClasspath().plus(sourceSets.getByName("main").getCompileClasspath());
                        sourceSet.getRuntimeClasspath().plus(sourceSets.getByName("main").getCompileClasspath());
                        sourceSet.getCompileClasspath().plus(sourceSets.getByName("main").getOutput());
                        sourceSet.getRuntimeClasspath().plus(sourceSets.getByName("main").getOutput());
                    });
                }

                if (templateProject.getPlatform() == Platform.COMMON) {
                    this.applyCommon(templateProject, target);
                }
                else if (templateProject.getPlatform() == Platform.FABRIC) {
                    this.applyFabric(templateProject, target);
                } else if (templateProject.getPlatform() == Platform.FORGE) {
                    this.applyForge(templateProject, target);
                }
            }
        });
    }

    private void applyCommon(TemplateProject templateProject, Project target) {
        this.validateLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "fabric-loom"));

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", project.getExtensions().getByType(LoomGradleExtensionAPI.class).officialMojangMappings());
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.property("fabric_loader_version"));

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
            });

            //noinspection UnstableApiUsage
            extension.getMixin().getUseLegacyMixinAp().set(false);

            if (project.hasProperty("access_widener_path")) {
                extension.getAccessWidenerPath().set(project.file(templateProject.property("access_widener_path")));
            }
        });
    }

    private void applyFabric(TemplateProject templateProject, Project target) {
        this.validateLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "fabric-loom"));
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();

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

    private void applyForge(TemplateProject templateProject, Project target) {
        this.validateForgeGradleVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "net.minecraftforge.gradle"));
        if (templateProject.usesMixins()) {
            this.validateMixinGradleVersionIfNeeded(target);
            SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
            project.apply(Map.of("plugin", "org.spongepowered.mixin"));
            project.getExtensions().configure(MixinExtension.class, extension -> {
                extension.add(sourceSets.getByName("main"), templateProject.property("mod_id") + ".refmap.json");
                extension.disableAnnotationProcessorCheck();
            });
        }

        project.getExtensions().configure(UserDevExtension.class, extension -> {
            extension.mappings("official", Constants.MINECRAFT_VERSION);

            if (templateProject.usesAccessTransformers()) {
                extension.accessTransformer(project.file("src/common/resources/META-INF/accesstransformer.cfg"));
            }
        });

        project.getDependencies().add("minecraft", "net.minecraftforge:forge:" + Constants.MINECRAFT_VERSION + "-" + templateProject.property("forge_version"));
        if (templateProject.usesMixins()) {
            project.getDependencies().add("implementation", "org.spongepowered:mixin:" + templateProject.property("mixin_version"));
            project.getDependencies().add("annotationProcessor", "org.spongepowered:mixin:" + templateProject.property("mixin_version") + ":processor");
        }
        //noinspection UnstableApiUsage
        project.getTasks().withType(ProcessResources.class).configureEach(task -> {
            HashMap<String, String> props = new HashMap<>();
            props.put("version", templateProject.property("mod_version"));
            task.getInputs().properties(props);
            task.filesMatching("META-INF/mods.toml", details -> details.expand(props));
            task.exclude(".cache/*");
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

    private void validateForgeGradleVersionIfNeeded(Project target) {
        if (!validatedForgeGradleVersion) {
            Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : artifacts) {
                ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
                if (identifier.getGroup().equals("net.minecraftforge.gradle") && identifier.getName().equals("ForgeGradle")) {
                    String forgeGradleVersion = identifier.getVersion();
                    if (!forgeGradleVersion.equals(Constants.REQUIRED_FORGE_GRADLE_VERSION)) {
                        throw new IllegalStateException("This plugin requires forge gradle " + Constants.REQUIRED_FORGE_GRADLE_VERSION + ", current is " + forgeGradleVersion + ".");
                    } else {
                        validatedForgeGradleVersion = true;
                        return;
                    }
                }
            }
            throw new IllegalStateException("This plugin requires forge gradle, add it to the current project un-applied.");
        }
    }

    private void validateMixinGradleVersionIfNeeded(Project target) {
        if (!validatedMixinGradleVersion) {
            Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : artifacts) {
                ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
                if (identifier.getGroup().equals("org.spongepowered") && identifier.getName().equals("mixingradle")) {
                    String mixinGradleVersion = identifier.getVersion();
                    if (!mixinGradleVersion.equals(Constants.REQUIRED_MIXIN_GRADLE_VERSION)) {
                        throw new IllegalStateException("This plugin requires mixin gradle " + Constants.REQUIRED_MIXIN_GRADLE_VERSION + ", current is " + mixinGradleVersion + ".");
                    } else {
                        validatedMixinGradleVersion = true;
                        return;
                    }
                }
            }
            throw new IllegalStateException("This plugin requires mixin gradle, add it to the current project un-applied.");
        }
    }
}
