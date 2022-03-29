package ninjaphenix.gradle.mod.impl.ext;

import ninjaphenix.gradle.mod.impl.Constants;
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.impl.DependencyDownloadHelper;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;

public class ModGradleExtensionImpl implements ModGradleExtension {
    private DependencyDownloadHelper helper;
    private final Project project;

    public  ModGradleExtensionImpl(Project project) {
        this.project = project;
    }
    @Override
    public String getMinecraftVersion() {
        return Constants.MINECRAFT_VERSION;
    }

    @Override
    public JavaVersion getJavaVersion() {
        return Constants.JAVA_VERSION;
    }

    public DependencyDownloadHelper getDependencyDownloadHelper() {
        if (helper != null) {
            return helper;
        }
        if (project == project.getRootProject()) {
            helper = new DependencyDownloadHelper();
        } else {
            helper = project.getRootProject().getExtensions().getByType(ModGradleExtension.class).getDependencyDownloadHelper();
        }
        return helper;
    }
}
