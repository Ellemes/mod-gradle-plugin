package ninjaphenix.gradle.mod;

public enum Platform {
    FABRIC,
    FORGE;

    public static Platform of(String name) {
        return switch(name) {
            case "fabric" -> Platform.FABRIC;
            case "forge" -> Platform.FORGE;
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
