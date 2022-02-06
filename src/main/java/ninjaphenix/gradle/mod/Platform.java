package ninjaphenix.gradle.mod;

public enum Platform {
    COMMON,
    FABRIC,
    FORGE;

    public static Platform of(String name) {
        return switch(name) {
            case "common" -> Platform.COMMON;
            case "fabric" -> Platform.FABRIC;
            case "forge" -> Platform.FORGE;
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
