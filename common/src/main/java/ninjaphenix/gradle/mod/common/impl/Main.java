package ninjaphenix.gradle.mod.common.impl;

import dev.architectury.plugin.ArchitectPluginExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import ninjaphenix.gradle.mod.utils.impl.Constants;
import ninjaphenix.gradle.mod.utils.impl.project.Platform;
import ninjaphenix.gradle.mod.utils.impl.project.PlatformLogic;
import ninjaphenix.gradle.mod.utils.impl.project.TemplateProject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.Map;

public class Main implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        TemplateProject templateProject = new TemplateProject(project);
        Main.applyArchLoom(project);
        PlatformLogic.apply(templateProject);
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

        //Main.applyArchPlugin(project, templateProject.getPlatform());
    }

    private static void applyArchLoom(Project project) {
        project.apply(Map.of("plugin", "dev.architectury.loom"));
        LoomGradleExtensionAPI loomPlugin = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        loomPlugin.silentMojangMappingsLicense();

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", loomPlugin.officialMojangMappings());
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
