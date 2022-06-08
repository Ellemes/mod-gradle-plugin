package ellemes.gradle.mod.impl;

public enum Platform {
    COMMON("common"),
    FABRIC("fabric"),
    FORGE("forge"),
    QUILT("quilt");

    private final String name;

    Platform(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Platform of(String name) {
        return switch (name) {
            case "common" -> Platform.COMMON;
            case "fabric" -> Platform.FABRIC;
            case "forge" -> Platform.FORGE;
            case "quilt" -> Platform.QUILT;
            default -> throw new IllegalStateException("Unexpected mod platform: " + name);
        };
    }

    public String getModInfoFile() {
        return switch (this) {
            case FABRIC -> "fabric.mod.json";
            case FORGE -> "META-INF/mods.toml";
            case QUILT -> "quilt.mod.json";
            default -> null;
        };
    }
}
