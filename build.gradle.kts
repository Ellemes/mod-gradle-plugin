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
version = "0.0.5"

repositories {
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
}

dependencies {
    compileOnly("net.fabricmc:fabric-loom:0.11.29")
}

