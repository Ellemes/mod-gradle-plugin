package ninjaphenix.gradle.mod.utils.api.ext;

import ninjaphenix.gradle.mod.utils.impl.dependency.DependencyDownloadHelper;
import org.gradle.api.JavaVersion;

public interface ModGradleExtension {
    String getMinecraftVersion();

    JavaVersion getJavaVersion();

    // Not sure why I'm exposing this but doesn't do any harm
    DependencyDownloadHelper getDependencyDownloadHelper();

    void fabricApi(String... modules);

    void qsl(String... modules);
}
