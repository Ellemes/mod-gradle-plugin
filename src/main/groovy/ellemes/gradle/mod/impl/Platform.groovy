package ellemes.gradle.mod.impl

enum Platform {
    COMMON("common"),
    FABRIC("fabric"),
    FORGE("forge"),
    QUILT("quilt");

    private final String name

    Platform(String name) {
        this.name = name
    }

    String getName() {
        return name
    }

    static Platform of(String name) {
        if (name == "common") {
            COMMON
        } else if (name == "fabric") {
            FABRIC
        } else if (name == "forge") {
            FORGE
        } else if (name == "quilt") {
            QUILT
        }
// todo: groovy 4.0
//        return switch (name) {
//            case "common" -> Platform.COMMON;
//            case "fabric" -> Platform.FABRIC;
//            case "forge" -> Platform.FORGE;
//            case "quilt" -> Platform.QUILT;
//            default -> throw new IllegalStateException("Unexpected mod platform: " + name);
//        };
    }

    String getModInfoFile() {
        if (this == FABRIC) {
            "fabric.mod.json"
        } else if (this == FORGE) {
            "META-INF/mods.toml"
        } else if (this == QUILT) {
            "quilt.mod.json"
        }
        null
// todo: groovy 4.0
//        return switch (this) {
//            case FABRIC -> "fabric.mod.json";
//            case FORGE -> "META-INF/mods.toml";
//            case QUILT -> "quilt.mod.json";
//            default -> null;
//        };
    }
}
