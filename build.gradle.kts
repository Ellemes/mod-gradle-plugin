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
version = "6.2.2.0"

repositories {
    maven {
        name = "Architectury Maven"
        url = uri("https://maven.architectury.dev")
    }
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("dev.architectury:architectury-loom:0.11.0-SNAPSHOT")
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        implementation("org.ow2.asm:${it}:9.2")
    }
}

