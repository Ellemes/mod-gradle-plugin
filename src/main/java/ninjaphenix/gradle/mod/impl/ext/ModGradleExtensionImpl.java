package ninjaphenix.gradle.mod.impl.ext;

import ninjaphenix.gradle.mod.impl.Constants;
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
import ninjaphenix.gradle.mod.impl.DependencyDownloadHelper;
import org.gradle.api.JavaVersion;

public class ModGradleExtensionImpl implements ModGradleExtension {
    private final DependencyDownloadHelper helper;

    public ModGradleExtensionImpl() {
        this.helper = new DependencyDownloadHelper();
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
        return helper;
    }
}
