package ninjaphenix.gradle.mod.impl;

import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.minecraftforge.gradle.common.util.MojangLicenseHelper;
import net.minecraftforge.gradle.userdev.UserDevExtension;
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
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.DefaultTaskExecutionRequest;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.gradle.plugins.MixinExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

// Acknowledgements:
//  Common project inspired by https://github.com/samolego/MultiLoaderTemplate
@SuppressWarnings("unused")
public final class GradlePlugin implements Plugin<Project> {
    private final AtomicBoolean validatedFabricLoomVersion = new AtomicBoolean(false);
    private final AtomicBoolean validatedQuiltLoomVersion = new AtomicBoolean(false);
    private final AtomicBoolean validatedForgeGradleVersion = new AtomicBoolean(false);
    private final AtomicBoolean validatedMixinGradleVersion = new AtomicBoolean(false);

    @Override
    public void apply(@NotNull Project target) {
        this.validateGradleVersion(target);

        Task buildTask = target.task("buildMod");

        target.getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl());

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

                project.getTasks().withType(JavaCompile.class).configureEach(task -> task.getOptions().setEncoding("UTF-8"));

                project.getDependencies().add("implementation", "org.jetbrains:annotations:" + Constants.JETBRAINS_ANNOTATIONS_VERSION);

                if (templateProject.producesReleaseArtifact()) {
                    buildTask.dependsOn(project.getTasks().getByName("build"));
                }

                templateProject.getCommonProject().ifPresent(common -> {
                    SourceSet mainSourceSet = common.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main");
                    //noinspection UnstableApiUsage,CodeBlock2Expr
                    project.getTasks().withType(ProcessResources.class).configureEach(task -> {
                        task.from(mainSourceSet.getResources());
                    });

                    //noinspection CodeBlock2Expr
                    project.getTasks().withType(JavaCompile.class).configureEach(task -> {
                        task.source(mainSourceSet.getAllSource());

                    });

                    project.getDependencies().add("compileOnly", common);
                });

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
    }

    private void applyCommon(TemplateProject templateProject, Project target) {
        this.validateFabricLoomVersionIfNeeded(target);
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
        this.validateFabricLoomVersionIfNeeded(target);
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
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                        settings.runDir("build/fabric-datagen");
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

    private void applyQuilt(TemplateProject templateProject, Project target) {
        this.validateQuiltLoomVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "org.quiltmc.loom"));
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", project.getExtensions().getByType(LoomGradleExtensionAPI.class).officialMojangMappings());
        dependencies.add("modImplementation", "org.quiltmc:quilt-loader:" + templateProject.property("quilt_loader_version"));
        if (project.hasProperty("qsl_version")) {
            dependencies.add("modImplementation", "org.quiltmc.qsl:qsl:" + templateProject.property("qsl_version"));
        }
        if (project.hasProperty("fabric_api_version")) {
            dependencies.add("modImplementation", "org.quiltmc.fabric_api_qsl:fabric-api:" + templateProject.property("fabric_api_version"));
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
                        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
                        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
                        settings.runDir("build/quilt-datagen");
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
            task.filesMatching("quilt.mod.json", details -> details.expand(props));
            task.exclude(".cache/*");
        });
    }

    private void applyForge(TemplateProject templateProject, Project target) {
        this.validateForgeGradleVersionIfNeeded(target);
        Project project = templateProject.getProject();
        project.apply(Map.of("plugin", "net.minecraftforge.gradle"));
        project.getGradle().getStartParameter().getTaskRequests().add(0, new DefaultTaskExecutionRequest(List.of(":" + project.getName() + ":" + MojangLicenseHelper.HIDE_LICENSE)));
        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        if (templateProject.usesMixins()) {
            this.validateMixinGradleVersionIfNeeded(target);
            project.apply(Map.of("plugin", "org.spongepowered.mixin"));
            project.getExtensions().configure(MixinExtension.class, extension -> {
                extension.add(sourceSets.getByName("main"), templateProject.property("mod_id") + ".refmap.json");
                extension.disableAnnotationProcessorCheck();
            });
        }

        project.getExtensions().configure(UserDevExtension.class, extension -> {
            extension.mappings("official", Constants.MINECRAFT_VERSION);

            extension.getRuns().create("client", config -> {
                config.workingDirectory(target.file("run"));
                config.getMods().create(templateProject.property("mod_id"), modConfig -> modConfig.source(sourceSets.getByName("main")));
            });

            extension.getRuns().create("server", config -> {
                config.workingDirectory(target.file("run"));
                config.getMods().create(templateProject.property("mod_id"), modConfig -> modConfig.source(sourceSets.getByName("main")));
            });

            if (templateProject.usesDataGen()) {
                extension.getRuns().create("data", config -> {
                    config.workingDirectory(target.file("run"));
                    //noinspection CodeBlock2Expr
                    config.getMods().create(templateProject.property("mod_id"), modConfig -> {
                        modConfig.source(sourceSets.getByName("main"));
                    });
                    List<Object> args = new ArrayList<>(List.of("--mod", templateProject.property("mod_id"), "--all",
                            "--output", project.file("src/main/generated"),
                            "--existing", project.file("src/main/resources")));
                    templateProject.getCommonProject().ifPresent(common -> {
                        args.add("--existing");
                        args.add(common.file("src/main/resources"));
                    });
                    config.args(args);
                });
            }

            if (templateProject.usesAccessTransformers()) {
                extension.accessTransformer(project.file("src/main/resources/META-INF/accesstransformer.cfg"));
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
        // todo: this check doesn't work
        boolean isCorrectGradleVersion = target.getGradle().getGradleVersion().equals(Constants.REQUIRED_GRADLE_VERSION);
        List<String> tasks = target.getGradle().getStartParameter().getTaskNames();
        boolean isExecutingWrapperTaskOnly = tasks.size() == 1 && tasks.get(0).equals(":wrapper");
        if (!isCorrectGradleVersion && !isExecutingWrapperTaskOnly) {
            throw new IllegalStateException("This plugin requires gradle " + Constants.REQUIRED_GRADLE_VERSION + " to update run: ./gradlew :wrapper --gradle-version " + Constants.REQUIRED_GRADLE_VERSION);
        }
    }

    private void validateFabricLoomVersionIfNeeded(Project target) {
        this.validatePluginVersionIfNeeded(target, validatedFabricLoomVersion, "net.fabricmc", "fabric-loom", Constants.REQUIRED_FABRIC_LOOM_VERSION, "fabric loom");
    }

    private void validateQuiltLoomVersionIfNeeded(Project target) {
        // todo: change plugin group / name if needed
        this.validatePluginVersionIfNeeded(target, validatedFabricLoomVersion, "org.quiltmc", "loom", Constants.REQUIRED_QUILT_LOOM_VERSION, "quilt loom");
    }

    private void validateForgeGradleVersionIfNeeded(Project target) {
        this.validatePluginVersionIfNeeded(target, validatedForgeGradleVersion, "net.minecraftforge.gradle", "ForgeGradle", Constants.REQUIRED_FORGE_GRADLE_VERSION, "forge gradle");
    }

    private void validateMixinGradleVersionIfNeeded(Project target) {
        this.validatePluginVersionIfNeeded(target, validatedMixinGradleVersion, "org.spongepowered", "mixingradle", Constants.REQUIRED_MIXIN_GRADLE_VERSION, "mixin gradle");
    }

    private void validatePluginVersionIfNeeded(Project target, AtomicBoolean checked, String group, String name, String requiredVersion, String friendlyName) {
        if (!checked.get()) {
            Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
            for (ResolvedArtifact artifact : artifacts) {
                ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
                if (identifier.getGroup().equals(group) && identifier.getName().equals(name)) {
                    String mixinGradleVersion = identifier.getVersion();
                    if (!mixinGradleVersion.equals(requiredVersion)) {
                        throw new IllegalStateException("This plugin requires " + friendlyName + requiredVersion + ", current is " + mixinGradleVersion + ".");
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
