package ninjaphenix.gradle.mod.api.logic;

import dev.architectury.plugin.ArchitectPluginExtension;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.impl.Constants;
import ninjaphenix.gradle.mod.impl.Platform;
import ninjaphenix.gradle.mod.impl.ext.ModGradleExtensionImpl;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.net.URISyntaxException;
import java.util.Map;

public class ArchLogic {
    static void applyLoom(Project project) {
        project.apply(Map.of("plugin", "dev.architectury.loom"));
        LoomGradleExtensionAPI loomPlugin = project.getExtensions().getByType(LoomGradleExtensionAPI.class);
        loomPlugin.silentMojangMappingsLicense();

        DependencyHandler dependencies = project.getDependencies();
        dependencies.add("minecraft", "com.mojang:minecraft:" + Constants.MINECRAFT_VERSION);
        dependencies.add("mappings", loomPlugin.officialMojangMappings());
    }

    static void applyPlugin(Project project, Platform platform) {
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

    public static void applyRoot(Project project) {
        DependencyDownloadHelper helper = null;
        try {
            helper = new DependencyDownloadHelper(project.getProjectDir().toPath().resolve(".gradle/mod-cache/"));
        } catch (URISyntaxException ignored) {
        }
        if (helper == null) throw new IllegalStateException("Bad coder exception.");
        project.apply(Map.of("plugin", "architectury-plugin"));
        project.getExtensions().configure(ArchitectPluginExtension.class, extension -> extension.setMinecraft(Constants.MINECRAFT_VERSION));
        Task buildTask = project.task("buildMod");

        project.getExtensions().add(ModGradleExtension.class, "mod", new ModGradleExtensionImpl(project, helper));
    }
}
