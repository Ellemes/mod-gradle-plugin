package ellemes.gradle.mod.impl

final class Constants {
    public static final String JETBRAINS_ANNOTATIONS_VERSION = "@JETBRAINS_ANNOTATIONS_VERSION@"
    public static final String REQUIRED_GRADLE_VERSION = "@REQUIRED_GRADLE_VERSION@"

    static final class Keys {
        static final class Template {
            public static final String PLATFORM = "template.platform"
            public static final String COMMON_PROJECT = "template.commonProject"
            public static final String MAVEN_ARTIFACT_ID = "template.maven_artifact_id"
            public static final String EXTRA_MOD_INFO_REPLACEMENTS = "template.extraModInfoReplacements"
            // Internal
            public static final String PROJECT = "template.project"
        }

        public static final String JAVA_VERSION = "java_version"
        public static final String ACCESS_WIDENER = "access_widener_path"
        public static final String FABRIC_LOADER_VERSION = "fabric_loader_version"
        public static final String FABRIC_API_VERSION = "fabric_api_version"
        public static final String QUILT_LOADER_VERSION = "quilt_loader_version"
        public static final String QSL_VERSION = "qsl_version"
        public static final String FORGE_VERSION = "forge_version"
    }

    private Constants() {
        throw new IllegalStateException("Should not instantiate utility class.")
    }
}
