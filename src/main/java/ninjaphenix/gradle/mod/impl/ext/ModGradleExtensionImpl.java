package ninjaphenix.gradle.mod.impl.ext;

import ninjaphenix.gradle.mod.impl.Constants;
import ninjaphenix.gradle.mod.api.ext.ModGradleExtension;
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
