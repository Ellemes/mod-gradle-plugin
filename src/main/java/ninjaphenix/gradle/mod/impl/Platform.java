package ninjaphenix.gradle.mod.impl;

public enum Platform {
    COMMON,
    FABRIC,
    FORGE,
    QUILT;

    public static Platform of(String name) {
        return switch (name) {
            case "common" -> Platform.COMMON;
            case "fabric" -> Platform.FABRIC;
            case "forge" -> Platform.FORGE;
            case "quilt" -> Platform.QUILT;
            default -> throw new IllegalStateException("Unexpected mod platform: " + name);
        };
    }
}
