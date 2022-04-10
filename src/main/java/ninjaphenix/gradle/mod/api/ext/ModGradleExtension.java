package ninjaphenix.gradle.mod.api.ext;

import ninjaphenix.gradle.mod.impl.dependency.DependencyDownloadHelper;
import org.gradle.api.JavaVersion;

public interface ModGradleExtension {
    String getMinecraftVersion();

    JavaVersion getJavaVersion();

    // Not sure why I'm exposing this but doesn't do any harm
    DependencyDownloadHelper getDependencyDownloadHelper();

    void fabricApi(String... modules);

    void qsl(String... modules);
}
