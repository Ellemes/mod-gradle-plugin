package ninjaphenix.gradle.mod.root.impl;

import dev.architectury.plugin.ArchitectPluginExtension;
import ninjaphenix.gradle.mod.utils.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.utils.impl.Constants;
import ninjaphenix.gradle.mod.utils.impl.dependency.DependencyDownloadHelper;
import ninjaphenix.gradle.mod.utils.impl.ext.ModGradleExtensionImpl;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.net.URISyntaxException;
import java.util.Map;

public class Main implements Plugin<Project> {
    @Override
    public void apply(Project project) {
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
