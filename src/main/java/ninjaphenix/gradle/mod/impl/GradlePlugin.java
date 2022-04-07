package ninjaphenix.gradle.mod.impl;

import dev.architectury.plugin.ArchitectPluginExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.impl.ext.ModGradleExtensionImpl;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

// Acknowledgements:
//  Common project inspired by https://github.com/samolego/MultiLoaderTemplate
@SuppressWarnings("unused")
public final class GradlePlugin implements Plugin<Project> {
    private final AtomicBoolean validatedArchLoomVersion = new AtomicBoolean(false);
    private final AtomicBoolean validatedArchPluginVersion = new AtomicBoolean(false);
    private final AtomicBoolean validatedQuiltLoomVersion = new AtomicBoolean(false);
    private final DependencyDownloadHelper helper = new DependencyDownloadHelper();

    public GradlePlugin() throws URISyntaxException {
    }

    private void registerExtension(Project project) {
        project.getProject().getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(project, helper));
    }

    @Override
    public void apply(@NotNull Project target) {
        this.validateGradleVersion(target);
        this.validateArchPluginVersion(target);
        target.apply(Map.of("plugin", "architectury-plugin"));
        target.getExtensions().configure(ArchitectPluginExtension.class, extension -> extension.setMinecraft(Constants.MINECRAFT_VERSION));
        Task buildTask = target.task("buildMod");

        this.registerExtension(target);

        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                project.getExtensions().getExtraProperties().set(Constants.TEMPLATE_PROPERTY_KEY, templateProject);
                this.registerExtension(project);
                project.apply(Map.of("plugin", "java-library"));
                project.setGroup("ninjaphenix");
                project.setVersion(templateProject.property("mod_version") + "+" + Constants.MINECRAFT_VERSION);
                project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(templateProject.<String>property("archives_base_name"));
                project.setBuildDir(project.getRootDir().toPath().resolve("build/" + project.getName()));

                project.getExtensions().configure(JavaPluginExtension.class, extension -> {
                    extension.setSourceCompatibility(Constants.JAVA_VERSION);
                    extension.setTargetCompatibility(Constants.JAVA_VERSION);
                });

                project.getTasks().withType(JavaCompile.class).configureEach(task -> {
                    CompileOptions options = task.getOptions();
                    options.setEncoding("UTF-8");
                    options.getRelease().set(Constants.JAVA_VERSION.ordinal() + 1);
                });

                project.getRepositories().maven(repo -> {
                    repo.setName("Unofficial CurseForge Maven");
                    repo.setUrl("https://cursemaven.com");
                    repo.content(descriptor -> descriptor.includeGroup("curse.maven"));
                });

                project.getRepositories().maven(repo -> {
                    repo.setName("Modrinth Maven");
                    repo.setUrl("https://api.modrinth.com/maven");
                    repo.content(descriptor -> descriptor.includeGroup("maven.modrinth"));
                });

                project.getRepositories().mavenLocal();

                project.getDependencies().add("implementation", "org.jetbrains:annotations:" + Constants.JETBRAINS_ANNOTATIONS_VERSION);

                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.getTasks().getByName("build"));
                }

                if (templateProject.usesDataGen()) {
                    SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
                    sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/main/generated"));
                    project.getTasks().getByName("jar", task -> ((Jar) task).exclude("**/datagen"));
                }

                if (templateProject.getPlatform() == Platform.COMMON) {
                    this.applyCommon(templateProject, target);
                } else if (templateProject.getPlatform() == Platform.FABRIC) {
                    this.applyFabric(templateProject, target);
                } else if (templateProject.getPlatform() == Platform.QUILT) {
                    this.applyQuilt(templateProject, target);
                } else if (templateProject.getPlatform() == Platform.FORGE) {
                    this.applyForge(templateProject, target);
                }
            }
        });

        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                this.applyArchPlugin(project, templateProject.getPlatform());
            }
        });
    }

    private void applyCommon(TemplateProject templateProject, Project target) {
        this.validateArchLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        this.applyArchLoom(project);
        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.rootProperty("fabric_loader_version"));

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
            });

            if (project.hasProperty("access_widener_path")) {
                extension.getAccessWidenerPath().set(project.file(templateProject.property("access_widener_path")));
            }
        });
    }

    private void applyArchLoom(Project project) {
        project.apply(Map.of("plugin", "dev.architectury.loom"));
        LoomGradleExtensionAPI loomPlugin = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        loomPlugin.silentMojangMappingsLicense();

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", loomPlugin.officialMojangMappings());
    }

    private void applyArchPlugin(Project project, Platform platform) {
        project.apply(Map.of("plugin", "architectury-plugin"));
        var architecturyPlugin = project.getExtensions().getByType(ArchitectPluginExtension.class);
        switch (platform) {
            case COMMON -> {
                architecturyPlugin.common();
                architecturyPlugin.setInjectInjectables(false);
            }
            case FABRIC, QUILT -> architecturyPlugin.fabric();
            case FORGE -> architecturyPlugin.forge();
        }
    }

    private void applyFabric(TemplateProject templateProject, Project target) {
        this.validateArchLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "fabric");
        this.applyArchLoom(project);
        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.property("fabric_loader_version"));
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
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                        settings.runDir("build/" + project.getName() + "-datagen");
                    });
                }
            });
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

    private void applyQuilt(TemplateProject templateProject, Project target) {
        this.validateQuiltLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "org.quiltmc.loom"));
        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", project.getExtensions().getByType(LoomGradleExtensionAPI.class).officialMojangMappings());
        dependencies.add("modImplementation", "org.quiltmc:quilt-loader:" + templateProject.property("quilt_loader_version"));
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
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                        settings.runDir("build/" + project.getName() + "-datagen");
                    });
                }
            });
        });

        //noinspection UnstableApiUsage
        project.getTasks().withType(ProcessResources.class).configureEach(task -> {
            HashMap<String, String> props = new HashMap<>();
            props.put("version", templateProject.property("mod_version"));
            task.getInputs().properties(props);
            task.filesMatching("quilt.mod.json", details -> details.expand(props));
            task.exclude(".cache/*");
        });
    }

    private void applyForge(TemplateProject templateProject, Project target) {
        this.validateArchLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "forge");
        this.applyArchLoom(project);
        project.getDependencies().add("forge", "net.minecraftforge:forge:" + Constants.MINECRAFT_VERSION + "-" + templateProject.property("forge_version"));

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
                //if (templateProject.usesDataGen()) {
                //    container.create("datagen", settings -> {
                //        settings.client();
                //        settings.vmArg("-Dfabric-api.datagen");
                //        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                //        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                //        settings.runDir("build/" + project.getName() + "-datagen");
                //    });
                //}
            });
        });

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
        boolean isExecutingWrapperTaskOnly = tasks.size() == 3 && tasks.get(0).equals(":wrapper") && tasks.get(1).equals("--gradle-version") && tasks.get(2).equals(Constants.REQUIRED_GRADLE_VERSION);
        if (!isCorrectGradleVersion && !isExecutingWrapperTaskOnly) {
            throw new IllegalStateException("This plugin requires gradle " + Constants.REQUIRED_GRADLE_VERSION + " to update run: ./gradlew :wrapper --gradle-version " + Constants.REQUIRED_GRADLE_VERSION);
        }
    }

    private void validateArchPluginVersion(Project target) {
        this.validatePluginVersionIfNeeded(target, validatedArchPluginVersion, "architectury-plugin", "architectury-plugin.gradle.plugin", Constants.REQUIRED_ARCH_PLUGIN_VERSION, "arch plugin");
    }

    private void validateArchLoomVersionIfNeeded(Project target) {
        this.validatePluginVersionIfNeeded(target, validatedArchLoomVersion, "dev.architectury", "architectury-loom", Constants.REQUIRED_ARCH_LOOM_VERSION, "arch loom");
    }

    private void validateQuiltLoomVersionIfNeeded(Project target) {
        //Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
        //for (ResolvedArtifact artifact : artifacts) {
        //    ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
        //    target.getLogger().error(identifier.getGroup() + ":" + identifier.getName() + ":" + identifier.getVersion());
        //}
        this.validatePluginVersionIfNeeded(target, validatedQuiltLoomVersion, "org.quiltmc", "quilt-loom", Constants.REQUIRED_QUILT_LOOM_VERSION, "quilt loom");
    }

    private void validatePluginVersionIfNeeded(Project target, AtomicBoolean checked, String group, String name, String requiredVersion, String friendlyName) {
        if (!checked.get()) {
            Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : artifacts) {
                ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
                if (identifier.getGroup().equals(group) && identifier.getName().equals(name)) {
                    String pluginVersion = identifier.getVersion();
                    if (!pluginVersion.equals(requiredVersion)) {
                        throw new IllegalStateException("This plugin requires " + friendlyName + requiredVersion + ", current is " + pluginVersion + ".");
                    } else {
                        checked.set(true);
                        return;
                    }
                }
            }
            throw new IllegalStateException("This plugin requires " + friendlyName + ", add it to the current project un-applied.");
        }
    }
}
