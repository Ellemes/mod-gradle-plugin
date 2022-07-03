package ellemes.gradle.mod.impl;

import java.util.Locale;

public final class Constants {
    public static final String JETBRAINS_ANNOTATIONS_VERSION = "@JETBRAINS_ANNOTATIONS_VERSION@";

    public static final String REQUIRED_GRADLE_VERSION = "@REQUIRED_GRADLE_VERSION@";

    public static final String TEMPLATE_PLATFORM_KEY = "template.platform";
    public static final String TEMPLATE_COMMON_PROJECT_KEY = "template.commonProject";
    public static final String TEMPLATE_ENABLED_PLATFORMS_KEY = "template.enabled_platforms";

    public static final String TEMPLATE_PROPERTY_KEY = "template_project";
    public static final String MINECRAFT_VERSION_KEY = "minecraft_version";
    public static final String JAVA_VERSION_KEY = "java_version";
    public static final String MOD_ID_KEY = "mod_id";
    public static final String MOD_VERSION_KEY = "mod_version";
    public static final String FABRIC_API_VERSION_KEY = "fabric_api_version";
    public static final String QSL_VERSION_KEY = "qsl_version";

    public static final String MOD_UPLOAD_TASK = "releaseMod";
    public static final String MOD_BUILD_TASK = "buildMod";

    private Constants() {
        throw new IllegalStateException("Should not instantiate utility class.");
    }

    public static String titleCase(String input) {
        return input.substring(0, 1).toUpperCase(Locale.ROOT) + input.substring(1);
    }
}
