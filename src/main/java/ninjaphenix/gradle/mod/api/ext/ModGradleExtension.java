package ninjaphenix.gradle.mod.api.ext;

import ninjaphenix.gradle.mod.impl.DependencyDownloadHelper;
import org.gradle.api.JavaVersion;

public interface ModGradleExtension {
    String getMinecraftVersion();

    JavaVersion getJavaVersion();

    DependencyDownloadHelper getDependencyDownloadHelper();
}
