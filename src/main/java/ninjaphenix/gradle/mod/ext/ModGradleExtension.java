package ninjaphenix.gradle.mod.ext;

import org.gradle.api.JavaVersion;

public interface ModGradleExtension {
    String getMinecraftVersion();

    JavaVersion getJavaVersion();
}
