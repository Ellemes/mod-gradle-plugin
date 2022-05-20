package ellemes.gradle.mod.impl;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import dev.architectury.plugin.ArchitectPluginExtension;
import ellemes.gradle.mod.impl.dependency.DependencyDownloadHelper;
import ellemes.gradle.mod.impl.ext.ModGradleExtensionImpl;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.task.RemapJarTask;
import ellemes.gradle.mod.api.ext.ModGradleExtension;
import ellemes.gradle.mod.api.task.MinifyJsonTask;
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
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Acknowledgements:
//  Common project inspired by https://github.com/samolego/MultiLoaderTemplate
@SuppressWarnings("unused")
public final class GradlePlugin implements Plugin<Project> {
    private final AtomicBoolean validatedArchLoomVersion = new AtomicBoolean(false);
    private final AtomicBoolean validatedArchPluginVersion = new AtomicBoolean(false);
    private DependencyDownloadHelper helper;
    private String minecraftVersion;
    private JavaVersion javaVersion;

    private ModGradleExtension registerExtension(Project project) {
        project.getProject().getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(project, helper));
        return project.getProject().getExtensions().getByType(ModGradleExtension.class);
    }

    @Override
    public void apply(@NotNull Project target) {
        this.validateGradleVersion(target);
        try {
            helper = new DependencyDownloadHelper(target.getProjectDir().toPath().resolve(".gradle/mod-cache/"));
        } catch (URISyntaxException ignored) {
        }
        target.apply(Map.of("plugin", "architectury-plugin"));
        var rootModExtension = this.registerExtension(target);
        minecraftVersion = (String) target.getExtensions().getExtraProperties().get("minecraft_version");
        if (minecraftVersion == null) {
            throw new IllegalStateException("Property minecraft_version is missing.");
        }
        javaVersion = JavaVersion.toVersion(target.getExtensions().getExtraProperties().get("java_version"));
        if (javaVersion == null) {
            throw new IllegalStateException("Property java_version is missing.");
        }
        target.getExtensions().configure(ArchitectPluginExtension.class, extension -> extension.setMinecraft(minecraftVersion));
        Task buildTask = target.task("buildMod");
        Task releaseTask = target.task("releaseMod");

        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                project.getExtensions().getExtraProperties().set(Constants.TEMPLATE_PROPERTY_KEY, templateProject);
                this.registerExtension(project);
                project.apply(Map.of("plugin", "java-library"));
                project.setGroup("ellemes");
                project.setVersion(templateProject.property("mod_version") + "+" + minecraftVersion);
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
                    SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
                    sourceSets.named("main", sourceSet -> sourceSet.getResources().srcDir("src/main/generated"));
                    project.getTasks().getByName("jar", task -> ((Jar) task).exclude("**/datagen"));
                }

                if (templateProject.producesReleaseArtifact()) {
                    project.apply(Map.of("plugin", "com.modrinth.minotaur"));
                    project.apply(Map.of("plugin", "me.hypherionmc.cursegradle"));
                    var projectReleaseTask = project.task("releaseMod");
                    releaseTask.dependsOn(projectReleaseTask);
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

                templateProject.ifCommonProjectPresent(common -> {
                    project.apply(Map.of("plugin", "com.github.johnrengelman.shadow"));
                    ConfigurationContainer configurations = project.getConfigurations();
                    Configuration commonConfiguration = configurations.create("common");
                    Configuration shadowCommonConfiguration = configurations.create("shadowCommon");
                    configurations.named("compileClasspath").get().extendsFrom(commonConfiguration);
                    configurations.named("runtimeClasspath").get().extendsFrom(commonConfiguration);

                    ((Jar) project.getTasks().getByName("jar")).getArchiveClassifier().set("dev");

                    ShadowJar shadowJar = (ShadowJar) project.getTasks().getByName("shadowJar");
                    shadowJar.exclude("architectury.common.json"); // todo: useless?
                    shadowJar.setConfigurations(List.of(shadowCommonConfiguration));
                    shadowJar.getArchiveClassifier().set("dev-shadow");

                    RemapJarTask remapJarTask = (RemapJarTask) project.getTasks().getByName("remapJar");
                    if (templateProject.getPlatform() != Platform.FORGE) {
                        remapJarTask.getInjectAccessWidener().set(true);
                    }
                    remapJarTask.getInputFile().set(shadowJar.getArchiveFile());
                    remapJarTask.dependsOn(shadowJar);
                    remapJarTask.getArchiveClassifier().set("fat");

                    AdhocComponentWithVariants variants = (AdhocComponentWithVariants) project.getComponents().findByName("java");
                    variants.withVariantsFromConfiguration(project.getConfigurations().getByName("shadowRuntimeElements"), ConfigurationVariantDetails::skip);
                });
            }
        });

        target.subprojects(project -> {
            if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
                TemplateProject templateProject = new TemplateProject(project);
                this.applyArchPlugin(project, templateProject.getPlatform());
                templateProject.ifCommonProjectPresent(common -> {
                    if (common.hasProperty("access_widener_path")) {
                        project.getExtensions().getByType(LoomGradleExtensionAPI.class).getAccessWidenerPath().set(common.getExtensions().getByType(LoomGradleExtensionAPI.class).getAccessWidenerPath());
                    }
                    ConfigurationContainer configurations = project.getConfigurations();
                    String projectDisplayName = GradlePlugin.capitalize(project.getName());
                    configurations.named("development" + projectDisplayName).get().extendsFrom(configurations.getByName("common"));

                    DependencyHandler dependencies = project.getDependencies();
                    ModuleDependency commonDep = ((ProjectDependency) dependencies.project(Map.of("path", common.getPath(), "configuration", "namedElements"))).setTransitive(false);
                    ModuleDependency shadowCommonDep = ((ProjectDependency) dependencies.project(Map.of("path", common.getPath(), "configuration", "transformProduction"+projectDisplayName))).setTransitive(false);
                    dependencies.add("common", commonDep);
                    dependencies.add("shadowCommon", shadowCommonDep);
                });

                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.getTasks().getByName("build"));

                    var minJarTask = project.getTasks().register("minJar", MinifyJsonTask.class, task -> {
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
                        String projectDisplayName = GradlePlugin.capitalize(project.getName());
                        publications.create("maven" + projectDisplayName, MavenPublication.class, publication -> {
                            publication.setArtifactId(project.property("template.maven_artifact_id") + "-" + minecraftVersion +  "-" + project.getName());
                            publication.setVersion(templateProject.property("mod_version"));
                            var minifyJar = project.getTasks().getByName("minJar");
                            publication.artifact(minifyJar, it -> {
                                it.builtBy(minifyJar);
                                it.setClassifier("");
                            });
                        });
                    });
                }
            }
        });
    }

    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private void applyCommon(TemplateProject templateProject, Project target) {
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
        dependencies.add("minecraft", "com.mojang:minecraft:" + minecraftVersion);
        dependencies.add("mappings", loomPlugin.officialMojangMappings());
    }

    private void applyArchPlugin(Project project, Platform platform) {
        project.apply(Map.of("plugin", "architectury-plugin"));
        ArchitectPluginExtension extension = project.getExtensions().getByType(ArchitectPluginExtension.class);
        switch (platform) {
            case COMMON -> {
                extension.common(((String) project.getParent().property("template.enabled_platforms")).split(",")); // todo: fixme
                extension.setInjectInjectables(false);
            }
            case FABRIC, QUILT, FORGE -> extension.loader(platform.getName());
        }
    }

    private void applyFabric(TemplateProject templateProject, Project target) {
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
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "quilt");
        this.applyArchLoom(project);
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
        Project project = templateProject.getProject();
        project.getExtensions().getExtraProperties().set("loom.platform", "forge");
        this.applyArchLoom(project);
        project.getDependencies().add("forge", "net.minecraftforge:forge:" + minecraftVersion + "-" + templateProject.property("forge_version"));

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
}
