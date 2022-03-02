package ninjaphenix.gradle.mod.impl;

import org.gradle.api.JavaVersion;

public final class Constants {
    public static final JavaVersion JAVA_VERSION = JavaVersion.VERSION_17;
    public static final String JETBRAINS_ANNOTATIONS_VERSION = "23.0.0";
    public static final String MINECRAFT_VERSION = "1.18.2";

    public static final String REQUIRED_GRADLE_VERSION = "7.4";
    public static final String REQUIRED_LOOM_VERSION = "0.11.29";
    public static final String REQUIRED_FORGE_GRADLE_VERSION = "5.1.26";
    public static final String REQUIRED_MIXIN_GRADLE_VERSION = "0.7-SNAPSHOT";

    public static final String TEMPLATE_PLATFORM_KEY = "template.platform";
    public static final String TEMPLATE_COMMON_PROJECT_KEY = "template.commonProject";

    private Constants() {
        throw new IllegalStateException("Should not instantiate utility class.");
    }
}
