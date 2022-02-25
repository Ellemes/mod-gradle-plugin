package ninjaphenix.gradle.mod.ext;

import ninjaphenix.gradle.mod.Constants;
import org.gradle.api.JavaVersion;

public class ModGradleExtensionImpl implements ModGradleExtension {
    @Override
    public String getMinecraftVersion() {
        return Constants.MINECRAFT_VERSION;
    }

    @Override
    public JavaVersion getJavaVersion() {
        return Constants.JAVA_VERSION;
    }
}
