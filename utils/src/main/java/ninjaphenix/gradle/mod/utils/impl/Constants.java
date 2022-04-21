package ninjaphenix.gradle.mod.utils.impl;

import org.gradle.api.JavaVersion;

public final class Constants {
    public static final JavaVersion JAVA_VERSION = JavaVersion.VERSION_17;
    public static final String JETBRAINS_ANNOTATIONS_VERSION = "23.0.0";
    public static final String MINECRAFT_VERSION = "1.18.2";

    public static final String REQUIRED_GRADLE_VERSION = "7.4.2";
    public static final String REQUIRED_ARCH_LOOM_VERSION = "0.11.0-SNAPSHOT";
    public static final String REQUIRED_ARCH_PLUGIN_VERSION = "3.4-SNAPSHOT";
    public static final String REQUIRED_QUILT_LOOM_VERSION = "0.12-SNAPSHOT";

    public static final String TEMPLATE_PLATFORM_KEY = "template.platform";
    public static final String TEMPLATE_COMMON_PROJECT_KEY = "template.commonProject";

    public static final String TEMPLATE_PROPERTY_KEY = "template_project";
    public static final String FABRIC_API_VERSION_KEY = "fabric_api_version";
    public static final String QSL_VERSION_KEY = "qsl_version";

    private Constants() {
        throw new IllegalStateException("Should not instantiate utility class.");
    }
}
