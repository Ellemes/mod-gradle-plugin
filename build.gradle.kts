plugins {
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("modPlugin") {
            id = "ninjaphenix.gradle.mod"
            implementationClass = "ninjaphenix.gradle.mod.impl.GradlePlugin"
        }
    }
}

group = "ninjaphenix"
base.archivesName.set("mod-gradle-plugin")
version = "6.2.0.7"

repositories {
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
    maven {
        name = "SpongePowered"
        url = uri("https://repo.spongepowered.org/repository/maven-public/")
    }
    maven {
        name = "MinecraftForge"
        url = uri("https://maven.minecraftforge.net/")
    }
    mavenCentral()
}

dependencies {
    compileOnly("net.fabricmc:fabric-loom:0.11.29")
    compileOnly("net.minecraftforge.gradle:ForgeGradle:5.1.26")
    compileOnly("org.spongepowered:mixingradle:0.7-SNAPSHOT")
    implementation("org.jetbrains:annotations:23.0.0")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        implementation("org.ow2.asm:${it}:9.2")
    }
}

