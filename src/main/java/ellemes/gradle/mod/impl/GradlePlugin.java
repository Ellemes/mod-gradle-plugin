package ellemes.gradle.mod.impl;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import dev.architectury.plugin.ArchitectPluginExtension;
import ellemes.gradle.mod.api.ext.ModGradleExtension;
import ellemes.gradle.mod.api.task.MinifyJsonTask;
import ellemes.gradle.mod.impl.dependency.DependencyDownloadHelper;
import ellemes.gradle.mod.impl.ext.ModGradleExtensionImpl;
import ellemes.gradle.mod.impl.misc.Platform;
import ellemes.gradle.mod.impl.misc.TemplateProject;
import ellemes.gradle.mod.impl.task.ReleaseModTask;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.task.RemapJarTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Acknowledgements:
//  Common project inspired by https://github.com/samolego/MultiLoaderTemplate
@SuppressWarnings("unused")
public final class GradlePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        this.validateGradleVersion(target);
        DependencyDownloadHelper helper = new DependencyDownloadHelper(target.getProjectDir().toPath().resolve(".gradle/mod-cache/"));
        target.apply(Map.of("plugin", "architectury-plugin"));
        String minecraftVersion = (String) target.property(Constants.MINECRAFT_VERSION_KEY);
        if (minecraftVersion == null) {
            throw GradlePlugin.missingProperty(Constants.MINECRAFT_VERSION_KEY);
        }
        JavaVersion javaVersion = JavaVersion.toVersion(target.property(Constants.JAVA_VERSION_KEY));
        if (javaVersion == null) {
            throw GradlePlugin.missingProperty(Constants.JAVA_VERSION_KEY);
        }
        target.getExtensions().configure(ArchitectPluginExtension.class, extension -> extension.setMinecraft(minecraftVersion));
        Task buildTask = target.task(Constants.MOD_BUILD_TASK);
        Task releaseTask = target.getTasks().create(Constants.MOD_UPLOAD_TASK, ReleaseModTask.class, target.getProjectDir());
        target.getGradle().getTaskGraph().whenReady(graph -> {
            for (Task task : graph.getAllTasks()) {
                if (task instanceof ReleaseModTask releaseModTask) {
                    releaseModTask.doPreReleaseChecks();
                }
            }
        });

        final List<TemplateProject> children = new ArrayList<>();

        // Give every template project a TemplateProject class.
        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                project.getExtensions().getExtraProperties().set(Constants.TEMPLATE_PROPERTY_KEY, templateProject);

                if (templateProject.getPlatform() == Platform.COMMON) {
                    project.getExtensions().getExtraProperties().set(Constants.TEMPLATE_ENABLED_PLATFORMS_KEY, new HashSet<String>());
                }

                children.add(templateProject);
            }
        });

        // Set the common project for every template project.
        for (TemplateProject child : children) {
            if (child.getProject().hasProperty(Constants.TEMPLATE_COMMON_PROJECT_KEY)) {
                String[] commonProjectNames = child.<String>property(Constants.TEMPLATE_COMMON_PROJECT_KEY).split(",");
                Map<String, Project> childProjects = target.getChildProjects();
                Set<TemplateProject> commonProjects = Arrays.stream(commonProjectNames)
                                                            .map(childProjects::get)
                                                            .map(project -> (TemplateProject) project.property(Constants.TEMPLATE_PROPERTY_KEY))
                                                            .collect(Collectors.toSet());
                child.setCommonProjects(commonProjects);
            }
        }

        // Sort children such that commonest project is last.
        children.sort((a, b) -> {
            if (a.containsCommonProject(b)) {
                return -1;
            } else if (b.containsCommonProject(a)) {
                return 1;
            } else {
                return 0;
            }
        });

        // Create the enabled platforms property for common projects.
        for (TemplateProject child : children) {
            child.ifCommonProjectsPresent(common -> {
                HashSet<String> enabledPlatforms = common.property(Constants.TEMPLATE_ENABLED_PLATFORMS_KEY);
                Platform childPlatform = child.getPlatform();
                if (childPlatform != Platform.COMMON) {
                    enabledPlatforms.add(childPlatform.getName());
                }
            });
        }

        // Reverse the children so that common projects are configured first.
        Collections.reverse(children);

        for (TemplateProject child : children) {
            GradlePlugin.preApplyArchPlugin(child, helper, javaVersion, releaseTask);
        }

        for (TemplateProject child : children) {
            GradlePlugin.postApplyArchPlugin(child, buildTask);
        }
    }

    private static void preApplyArchPlugin(TemplateProject templateProject, DependencyDownloadHelper helper, JavaVersion javaVersion, Task releaseTask) {
        Project project = templateProject.getProject();
        Platform platform = templateProject.getPlatform();
        project.getExtensions().getExtraProperties().set(Constants.TEMPLATE_PROPERTY_KEY, templateProject);
        project.getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(templateProject, helper));
        project.apply(Map.of("plugin", "java-library"));
        project.setGroup("ellemes");
        project.setVersion(templateProject.property(Constants.MOD_VERSION_KEY) + "+" + templateProject.property(Constants.MINECRAFT_VERSION_KEY));
        project.getExtensions().getByType(BasePluginExtension.class).getArchivesName().set(templateProject.<String>property("archives_base_name"));
        project.setBuildDir(project.getRootDir().toPath().resolve("build/" + project.getName()));

        project.getExtensions().configure(JavaPluginExtension.class, extension -> {
            extension.setSourceCompatibility(javaVersion);
            extension.setTargetCompatibility(javaVersion);
        });

        project.getTasks().withType(JavaCompile.class).configureEach(task -> {
            CompileOptions options = task.getOptions();
            options.setEncoding("UTF-8");
            options.getRelease().set(javaVersion.ordinal() + 1);
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

        project.getDependencies().add("compileOnly", "org.jetbrains:annotations:" + Constants.JETBRAINS_ANNOTATIONS_VERSION);

        if (templateProject.usesDataGen()) {
            if (platform != Platform.FORGE) { // Arch does this for forge...
                SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
                sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/generated/resources"));
            }
            project.getTasks().getByName("jar", task -> ((Jar) task).exclude("**/datagen"));
        }

        if (templateProject.producesReleaseArtifact()) {
            project.apply(Map.of("plugin", "com.modrinth.minotaur"));
            project.apply(Map.of("plugin", "me.hypherionmc.cursegradle"));
            releaseTask.dependsOn(project.task(Constants.MOD_UPLOAD_TASK));
        }

        if (platform == Platform.COMMON) {
            GradlePlugin.applyCommon(templateProject);
        } else if (platform == Platform.FABRIC) {
            GradlePlugin.applyFabric(templateProject);
        } else if (platform == Platform.QUILT) {
            GradlePlugin.applyQuilt(templateProject);
        } else if (platform == Platform.FORGE) {
            GradlePlugin.applyForge(templateProject);
        }

        if (templateProject.hasCommonProjects()) {
            project.apply(Map.of("plugin", "com.github.johnrengelman.shadow"));
            ConfigurationContainer configurations = project.getConfigurations();
            Configuration commonConfiguration = configurations.create("common");
            Configuration shadowCommonConfiguration = configurations.create("shadowCommon");
            configurations.named("compileClasspath").get().extendsFrom(commonConfiguration);
            configurations.named("runtimeClasspath").get().extendsFrom(commonConfiguration);

            ((Jar) project.getTasks().getByName("jar")).getArchiveClassifier().set("dev");

            ShadowJar shadowJar = (ShadowJar) project.getTasks().getByName("shadowJar");
            shadowJar.setConfigurations(List.of(shadowCommonConfiguration));
            shadowJar.getArchiveClassifier().set("dev-shadow");

            RemapJarTask remapJarTask = (RemapJarTask) project.getTasks().getByName("remapJar");
            if (platform != Platform.FORGE) {
                remapJarTask.getInjectAccessWidener().set(true);
            }
            remapJarTask.getInputFile().set(shadowJar.getArchiveFile());
            remapJarTask.dependsOn(shadowJar);
            remapJarTask.getArchiveClassifier().set("fat");

            AdhocComponentWithVariants variants = (AdhocComponentWithVariants) project.getComponents().getByName("java");
            variants.withVariantsFromConfiguration(project.getConfigurations().getByName("shadowRuntimeElements"), ConfigurationVariantDetails::skip);
        }

        String modInfoFile = platform.getModInfoFile();
        if (modInfoFile != null) {
            //noinspection UnstableApiUsage
            project.getTasks().withType(ProcessResources.class).configureEach(task -> {
                HashMap<String, String> props = new HashMap<>();
                props.put("version", templateProject.property(Constants.MOD_VERSION_KEY));
                if (project.hasProperty("template.extraModInfoReplacements")) {
                    Map<String, String> extraProps = templateProject.property("template.extraModInfoReplacements");
                    if (extraProps != null) {
                        props.putAll(extraProps);
                    }
                }
                task.getInputs().properties(props);
                task.filesMatching(modInfoFile, details -> details.expand(props));
                task.exclude(".cache/*");
            });
        }
    }

    private static void postApplyArchPlugin(TemplateProject templateProject, Task buildTask) {
        Project project = templateProject.getProject();
        GradlePlugin.applyArchPlugin(templateProject);
        templateProject.ifCommonProjectsPresent(common -> {
            if (project.hasProperty(Constants.TEMPLATE_COPY_AW_KEY)) {
                if (common.getProject().getName().equals(templateProject.property(Constants.TEMPLATE_COPY_AW_KEY)) && common.getProject().hasProperty(Constants.ACCESS_WIDENER_PATH_KEY)) {
                    project.getExtensions().getByType(LoomGradleExtensionAPI.class).getAccessWidenerPath().set(common.getProject().getExtensions().getByType(LoomGradleExtensionAPI.class).getAccessWidenerPath());
                }
            }
            ConfigurationContainer configurations = project.getConfigurations();
            String projectDisplayName = Constants.titleCase(project.getName());
            DependencyHandler dependencies = project.getDependencies();
            ModuleDependency commonDep = ((ProjectDependency) dependencies.project(Map.of("path", common.getProject().getPath(), "configuration", "namedElements"))).setTransitive(false);
            if (templateProject.getPlatform() == Platform.COMMON) {
                dependencies.add("compileClasspath", commonDep);
            } else {
                configurations.named("development" + projectDisplayName).get().extendsFrom(configurations.getByName("common"));
                ModuleDependency shadowCommonDep = ((ProjectDependency) dependencies.project(Map.of("path", common.getProject().getPath(), "configuration", "transformProduction" + projectDisplayName))).setTransitive(false);
                dependencies.add("common", commonDep);
                dependencies.add("shadowCommon", shadowCommonDep);
            }
        });

        if (templateProject.producesReleaseArtifact()) {
            buildTask.dependsOn(project.getTasks().getByName("build"));

            TaskProvider<MinifyJsonTask> minJarTask = project.getTasks().register("minJar", MinifyJsonTask.class, task -> {
                Task remapJarTask = project.getTasks().getByName("remapJar");
                task.getInput().set(remapJarTask.getOutputs().getFiles().getSingleFile());
                task.getArchiveClassifier().set(project.getName());
                task.from(project.getRootDir().toPath().resolve("LICENSE"));
                task.dependsOn(remapJarTask);
            });

            project.getTasks().getByName("build").dependsOn(minJarTask);
        }

        if (templateProject.producesMavenArtifact()) {
            project.apply(Map.of("plugin", "maven-publish"));

            project.getExtensions().getByType(PublishingExtension.class).publications(publications -> {
                String projectDisplayName = Constants.titleCase(project.getName());
                publications.create("maven" + projectDisplayName, MavenPublication.class, publication -> {
                    publication.setArtifactId(project.property("template.maven_artifact_id") + "-" + project.property(Constants.MINECRAFT_VERSION_KEY) +  "-" + project.getName());
                    publication.setVersion(templateProject.property(Constants.MOD_VERSION_KEY));
                    var minifyJar = project.getTasks().getByName("minJar");
                    publication.artifact(minifyJar, it -> {
                        it.builtBy(minifyJar);
                        it.setClassifier("");
                    });
                });
            });
        }
    }


    private static IllegalStateException missingProperty(String property) {
        return new IllegalStateException("Missing property: " + property);
    }

    private static void applyCommon(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        GradlePlugin.applyArchLoom(templateProject);
        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.property("fabric_loader_version"));

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

    private static void applyArchLoom(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "dev.architectury.loom"));
        LoomGradleExtensionAPI loomPlugin = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        loomPlugin.silentMojangMappingsLicense();

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + templateProject.property(Constants.MINECRAFT_VERSION_KEY));
        dependencies.add("mappings", loomPlugin.officialMojangMappings());
    }

    private static void applyArchPlugin(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "architectury-plugin"));
        ArchitectPluginExtension extension = project.getExtensions().getByType(ArchitectPluginExtension.class);
        Platform platform = templateProject.getPlatform();
        switch (platform) {
            case COMMON -> {
                extension.common((templateProject.<HashSet<String>>property(Constants.TEMPLATE_ENABLED_PLATFORMS_KEY)));
                extension.setInjectInjectables(false);
            }
            case FABRIC, QUILT, FORGE -> extension.loader(platform.getName());
        }
    }

    private static void applyFabric(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "fabric");
        GradlePlugin.applyArchLoom(templateProject);
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
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/generated/resources"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property(Constants.MOD_ID_KEY));
                        settings.runDir("build/" + project.getName() + "-datagen");
                    });
                }
            });
        });
    }

    private static void applyQuilt(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "quilt");
        GradlePlugin.applyArchLoom(templateProject);
        project.getRepositories().maven(repo -> {
            repo.setName("Quilt Release Maven");
            repo.setUrl("https://maven.quiltmc.org/repository/release/");
        });
        project.getRepositories().maven(repo -> {
            repo.setName("Quilt Snapshot Maven");
            repo.setUrl("https://maven.quiltmc.org/repository/snapshot/");
        });
        DependencyHandler dependencies = project.getDependencies();
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
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/generated/resources"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property(Constants.MOD_ID_KEY));
                        settings.runDir("build/" + project.getName() + "-datagen");
                    });
                }
            });
        });
    }

    private static void applyForge(TemplateProject templateProject) {
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "forge");
        GradlePlugin.applyArchLoom(templateProject);
        project.getDependencies().add("forge", "net.minecraftforge:forge:" + templateProject.property(Constants.MINECRAFT_VERSION_KEY) + "-" + templateProject.property("forge_version"));

        project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
            if (templateProject.usesDataGen()) {
                extension.forge(forgeExtensionAPI -> {
                    forgeExtensionAPI.dataGen(dataGenConsumer -> {
                        dataGenConsumer.mod(templateProject.<String>property(Constants.MOD_ID_KEY));
                    });
                });
            }
            extension.runs(container -> {
                container.named("client", settings -> settings.ideConfigGenerated(false));
                container.named("server", settings -> {
                    settings.ideConfigGenerated(false);
                    settings.serverWithGui();
                });
                if (templateProject.usesDataGen()) {
                    container.named("data", settings -> {
                        settings.programArg("--existing");
                        settings.programArg(project.file("src/main/resources").getAbsolutePath());
                        templateProject.ifCommonProjectsPresent(commonProject -> {
                            settings.programArg("--existing");
                            settings.programArg(commonProject.getProject().file("src/main/resources").getAbsolutePath());
                        });
                        settings.runDir("build/" + project.getName() + "-datagen");
                    });
                }
            });
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
}
