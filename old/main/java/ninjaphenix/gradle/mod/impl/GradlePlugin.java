package ninjaphenix.gradle.mod.impl;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

// Acknowledgements:
//  Common project inspired by https://github.com/samolego/MultiLoaderTemplate
@SuppressWarnings("unused")
public final class GradlePlugin implements Plugin<Project> {
    //private final AtomicBoolean validatedArchLoomVersion = new AtomicBoolean(false);
    //private final AtomicBoolean validatedArchPluginVersion = new AtomicBoolean(false);
    //private final AtomicBoolean validatedQuiltLoomVersion = new AtomicBoolean(false);
    //private DependencyDownloadHelper helper;

    //private void registerExtension(Project project) {
    //    project.getProject().getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(project, helper));
    //}

    @Override
    public void apply(@NotNull Project target) {
        //this.validateGradleVersion(target);
        //this.validateArchPluginVersion(target);
        //try {
        //    helper = new DependencyDownloadHelper(target.getProjectDir().toPath().resolve(".gradle/mod-cache/"));
        //} catch (URISyntaxException ignored) {
        //}
        //target.apply(Map.of("plugin", "architectury-plugin"));
        //target.getExtensions().configure(ArchitectPluginExtension.class, extension -> extension.setMinecraft(Constants.MINECRAFT_VERSION));
        //Task buildTask = target.task("buildMod");

        //this.registerExtension(target);

        //target.subprojects(project -> {
        //    if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {

        //    }
        //});

        //target.subprojects(project -> {
        //    if (project.hasProperty(Constants.TEMPLATE_PLATFORM_KEY)) {
        //        TemplateProject templateProject = new TemplateProject(project);
        //        this.applyArchPlugin(project, templateProject.getPlatform());
        //    }
        //});
    }

    //private void applyFabric(TemplateProject templateProject, Project target) {
    //    this.validateArchLoomVersionIfNeeded(target);
    //    Project project = templateProject.getProject();
    //    project.getExtensions().getExtraProperties().set("loom.platform", "fabric");
    //    this.applyArchLoom(project);
    //    DependencyHandler dependencies = project.getDependencies();
    //    dependencies.add("modImplementation", "net.fabricmc:fabric-loader:" + templateProject.property("fabric_loader_version"));
    //    project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
    //        extension.runs(container -> {
    //            container.named("client", settings -> settings.ideConfigGenerated(false));
    //            container.named("server", settings -> {
    //                settings.ideConfigGenerated(false);
    //                settings.serverWithGui();
    //            });
    //            if (templateProject.usesDataGen()) {
    //                container.create("datagen", settings -> {
    //                    settings.client();
    //                    settings.vmArg("-Dfabric-api.datagen");
    //                    settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
    //                    settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
    //                    settings.runDir("build/" + project.getName() + "-datagen");
    //                });
    //            }
    //        });
    //    });

    //    //noinspection UnstableApiUsage
    //    project.getTasks().withType(ProcessResources.class).configureEach(task -> {
    //        HashMap<String, String> props = new HashMap<>();
    //        props.put("version", templateProject.property("mod_version"));
    //        task.getInputs().properties(props);
    //        task.filesMatching("fabric.mod.json", details -> details.expand(props));
    //        task.exclude(".cache/*");
    //    });
    //}

    //private void applyQuilt(TemplateProject templateProject, Project target) {
    //    this.validateQuiltLoomVersionIfNeeded(target);
    //    Project project = templateProject.getProject();
    //    project.apply(Map.of("plugin", "org.quiltmc.loom"));
    //    project.getRepositories().maven(repo -> {
    //        repo.setName("Quilt Snapshot Maven");
    //        repo.setUrl("https://maven.quiltmc.org/repository/snapshot/");
    //    });
    //    DependencyHandler dependencies = project.getDependencies();
    //    dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
    //    dependencies.add("mappings", project.getExtensions().getByType(LoomGradleExtensionAPI.class).officialMojangMappings());
    //    dependencies.add("modImplementation", "org.quiltmc:quilt-loader:" + templateProject.property("quilt_loader_version"));
    //    project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
    //        extension.runs(container -> {
    //            container.named("client", settings -> settings.ideConfigGenerated(false));
    //            container.named("server", settings -> {
    //                settings.ideConfigGenerated(false);
    //                settings.serverWithGui();
    //            });
    //            if (templateProject.usesDataGen()) {
    //                container.create("datagen", settings -> {
    //                    settings.client();
    //                    settings.vmArg("-Dfabric-api.datagen");
    //                    settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
    //                    settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
    //                    settings.runDir("build/" + project.getName() + "-datagen");
    //                });
    //            }
    //        });
    //    });

    //    //noinspection UnstableApiUsage
    //    project.getTasks().withType(ProcessResources.class).configureEach(task -> {
    //        HashMap<String, String> props = new HashMap<>();
    //        props.put("version", templateProject.property("mod_version"));
    //        task.getInputs().properties(props);
    //        task.filesMatching("quilt.mod.json", details -> details.expand(props));
    //        task.exclude(".cache/*");
    //    });
    //}

    //private void applyForge(TemplateProject templateProject, Project target) {
    //    this.validateArchLoomVersionIfNeeded(target);
    //    Project project = templateProject.getProject();
    //    project.getExtensions().getExtraProperties().set("loom.platform", "forge");
    //    this.applyArchLoom(project);
    //    project.getDependencies().add("forge", "net.minecraftforge:forge:" + Constants.MINECRAFT_VERSION + "-" + templateProject.property("forge_version"));

    //    project.getExtensions().configure(LoomGradleExtensionAPI.class, extension -> {
    //        extension.runs(container -> {
    //            container.named("client", settings -> settings.ideConfigGenerated(false));
    //            container.named("server", settings -> {
    //                settings.ideConfigGenerated(false);
    //                settings.serverWithGui();
    //            });
    //            //if (templateProject.usesDataGen()) {
    //            //    container.create("datagen", settings -> {
    //            //        settings.client();
    //            //        settings.vmArg("-Dfabric-api.datagen");
    //            //        settings.vmArg("-Dfabric-api.datagen.output-dir=" + project.file("src/main/generated"));
    //            //        settings.vmArg("-Dfabric-api.datagen.datagen.modid=" + templateProject.property("mod_id"));
    //            //        settings.runDir("build/" + project.getName() + "-datagen");
    //            //    });
    //            //}
    //        });
    //    });

    //    //noinspection UnstableApiUsage
    //    project.getTasks().withType(ProcessResources.class).configureEach(task -> {
    //        HashMap<String, String> props = new HashMap<>();
    //        props.put("version", templateProject.property("mod_version"));
    //        task.getInputs().properties(props);
    //        task.filesMatching("META-INF/mods.toml", details -> details.expand(props));
    //        task.exclude(".cache/*");
    //    });
    //}

    //private void validateGradleVersion(Project target) {
    //    boolean isCorrectGradleVersion = target.getGradle().getGradleVersion().equals(Constants.REQUIRED_GRADLE_VERSION);
    //    List<String> tasks = target.getGradle().getStartParameter().getTaskNames();
    //    boolean isExecutingWrapperTaskOnly = tasks.size() == 3 && tasks.get(0).equals(":wrapper") && tasks.get(1).equals("--gradle-version") && tasks.get(2).equals(Constants.REQUIRED_GRADLE_VERSION);
    //    if (!isCorrectGradleVersion && !isExecutingWrapperTaskOnly) {
    //        throw new IllegalStateException("This plugin requires gradle " + Constants.REQUIRED_GRADLE_VERSION + " to update run: ./gradlew :wrapper --gradle-version " + Constants.REQUIRED_GRADLE_VERSION);
    //    }
    //}

    //// todo: allow fixed version
    //private void validateArchPluginVersion(Project target) {
    //    this.validatePluginVersionIfNeeded(target, validatedArchPluginVersion, "architectury-plugin", "architectury-plugin.gradle.plugin", Constants.REQUIRED_ARCH_PLUGIN_VERSION, "arch plugin");
    //}

    //// todo: allow fixed version
    //private void validateArchLoomVersionIfNeeded(Project target) {
    //    this.validatePluginVersionIfNeeded(target, validatedArchLoomVersion, "dev.architectury", "architectury-loom", Constants.REQUIRED_ARCH_LOOM_VERSION, "arch loom");
    //}

    //// todo: allow version from snapshot or release maven
    //private void validateQuiltLoomVersionIfNeeded(Project target) {
    //    //Set<ResolvedArtifact> artifacts = new TreeSet<>(Comparator.comparing(it -> it.getModuleVersion().getId().getGroup(), String::compareTo));
    //    //artifacts.addAll(target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts());
    //    //for (ResolvedArtifact artifact : artifacts) {
    //    //    ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
    //    //    target.getLogger().error(identifier.getGroup() + ":" + identifier.getName() + ":" + identifier.getVersion());
    //    //}
    //    //this.validatePluginVersionIfNeeded(target, validatedQuiltLoomVersion, "org.quiltmc", "loom", Constants.REQUIRED_QUILT_LOOM_VERSION, "quilt loom");
    //}

    //private void validatePluginVersionIfNeeded(Project target, AtomicBoolean checked, String group, String name, String requiredVersion, String friendlyName) {
    //    if (!checked.get()) {
    //        Set<ResolvedArtifact> artifacts = target.getBuildscript().getConfigurations().getByName("classpath").getResolvedConfiguration().getResolvedArtifacts();
    //        for (ResolvedArtifact artifact : artifacts) {
    //            ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
    //            if (identifier.getGroup().equals(group) && identifier.getName().equals(name)) {
    //                String pluginVersion = identifier.getVersion();
    //                if (!pluginVersion.equals(requiredVersion)) {
    //                    throw new IllegalStateException("This plugin requires " + friendlyName + " " + requiredVersion + ", current is " + pluginVersion + ".");
    //                } else {
    //                    checked.set(true);
    //                    return;
    //                }
    //            }
    //        }
    //        throw new IllegalStateException("This plugin requires " + friendlyName + ", add it to the current project un-applied.");
    //    }
    //}
}
