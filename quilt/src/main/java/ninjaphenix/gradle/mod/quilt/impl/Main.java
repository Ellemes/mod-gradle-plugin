package ninjaphenix.gradle.mod.quilt.impl;

import dev.architectury.plugin.ArchitectPluginExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import ninjaphenix.gradle.mod.utils.impl.Constants;
import ninjaphenix.gradle.mod.utils.impl.project.Platform;
import ninjaphenix.gradle.mod.utils.impl.project.PlatformLogic;
import ninjaphenix.gradle.mod.utils.impl.project.TemplateProject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.util.HashMap;
import java.util.Map;

public class Main implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        //this.validateQuiltLoomVersionIfNeeded(target);
        TemplateProject templateProject = new TemplateProject(project);
        project.apply(Map.of("plugin", "org.quiltmc.loom"));
        PlatformLogic.apply(templateProject);
        project.getRepositories().maven(repo -> {
            repo.setName("Quilt Snapshot Maven");
            repo.setUrl("https://maven.quiltmc.org/repository/snapshot/");
        });
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

        //Main.applyArchPlugin(project, templateProject.getPlatform());
    }

    private static void applyArchPlugin(Project project, Platform platform) {
        project.apply(Map.of("plugin", "architectury-plugin"));
        var extension = project.getExtensions().getByType(ArchitectPluginExtension.class);
        switch (platform) {
            case COMMON -> {
                extension.common();
                extension.setInjectInjectables(false);
            }
            case FABRIC, QUILT -> extension.fabric();
            case FORGE -> extension.forge();
        }
    }
}
