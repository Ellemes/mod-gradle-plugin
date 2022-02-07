plugins {
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("modPlugin") {
            id = "ninjaphenix.gradle.mod"
            implementationClass = "ninjaphenix.gradle.mod.GradlePlugin"
        }
    }
}

group = "ninjaphenix"
base.archivesName.set("mod-gradle-plugin")
version = "0.0.7"

repositories {
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
    maven {
        name = "SpongePowered"
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
    }
}

dependencies {
    compileOnly("net.fabricmc:fabric-loom:0.11.29")
    compileOnly("org.spongepowered.gradle.vanilla:org.spongepowered.gradle.vanilla.gradle.plugin:0.2.1-SNAPSHOT")
}

