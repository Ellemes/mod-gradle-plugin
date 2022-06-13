package ellemes.gradle.mod.impl;

public final class Constants {
    public static final String JETBRAINS_ANNOTATIONS_VERSION = "@JETBRAINS_ANNOTATIONS_VERSION@";

    public static final String REQUIRED_GRADLE_VERSION = "@REQUIRED_GRADLE_VERSION@";

    public static final String TEMPLATE_PLATFORM_KEY = "template.platform";
    public static final String TEMPLATE_COMMON_PROJECT_KEY = "template.commonProject";
    public static final String TEMPLATE_EXTRA_MOD_INFO_REPLACEMENTS_KEY = "template.extraModInfoReplacements";

    public static final String MOD_VERSION_KEY = "mod_version";
    public static final String JAVA_VERSION_KEY = "java_version";
    public static final String MINECRAFT_VERSION_KEY = "minecraft_version";
    public static final String ACCESS_WIDENER_KEY = "access_widener_path";
    public static final String FABRIC_LOADER_VERSION_KEY = "fabric_loader_version";
    public static final String FABRIC_API_VERSION_KEY = "fabric_api_version";
    public static final String QUILT_LOADER_VERSION_KEY = "quilt_loader_version";
    public static final String QSL_VERSION_KEY = "qsl_version";
    public static final String FORGE_VERSION_KEY = "forge_version";

    private Constants() {
        throw new IllegalStateException("Should not instantiate utility class.");
    }
}
