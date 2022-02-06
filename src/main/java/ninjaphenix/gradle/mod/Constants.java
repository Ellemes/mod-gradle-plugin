package ninjaphenix.gradle.mod;

import org.gradle.api.JavaVersion;

public final class Constants {
    public static final JavaVersion JAVA_VERSION = JavaVersion.VERSION_17;
    public static final String JETBRAINS_ANNOTATIONS_VERSION = "23.0.0";
    public static final String MINECRAFT_VERSION = "1.18.1";
    public static final String YARN_VERSION = "12";

    public static final String REQUIRED_GRADLE_VERSION = "7.4-rc-2";
    public static final String REQUIRED_LOOM_VERSION = "0.11.29";

    public static final String TEMPLATE_PLATFORM_KEY = "template.platform";

    private Constants() {
        throw new IllegalStateException("Should not instantiate utility class.");
    }
}
