package ninjaphenix.gradle.mod.ext;

import ninjaphenix.gradle.mod.Constants;

public class ModGradleExtensionImpl implements ModGradleExtension {
    @Override
    public String getMinecraftVersion() {
        return Constants.MINECRAFT_VERSION;
    }
}
