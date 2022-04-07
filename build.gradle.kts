plugins {
    `java-gradle-plugin`
    `maven-publish`
}

// idk intellij is being funky with buildscripts
project.extensions.getByType(GradlePluginDevelopmentExtension::class).apply {
    plugins {
        create("modPlugin") {
            id = "ninjaphenix.gradle.mod"
            implementationClass = "ninjaphenix.gradle.mod.impl.GradlePlugin"
        }
    }
}

group = "ninjaphenix"
project.extensions.getByType(BasePluginExtension::class).apply {
    archivesName.set("mod-gradle-plugin")
}
version = "6.2.1.3"

repositories {
    maven {
        name = "Architectury Maven"
        url = uri("https://maven.architectury.dev")
    }
    mavenCentral()
}

dependencies {
    "compileOnly"("dev.architectury:architectury-loom:0.11.0-SNAPSHOT")
    "compileOnly"("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    "implementation"("org.jetbrains:annotations:23.0.0")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        "implementation"("org.ow2.asm:${it}:9.2")
    }
}

