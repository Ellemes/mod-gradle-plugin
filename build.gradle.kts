plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm").version("1.6.10")
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(JavaVersion.VERSION_17.ordinal + 1)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

group = "ninjaphenix"
base.archivesName.set("mod-gradle-plugin")
version = "6.2.1.1"

repositories {
    maven {
        name = "Architectury Maven"
        url = uri("https://maven.architectury.dev")
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly("dev.architectury:architectury-loom:0.11.0-SNAPSHOT")
    compileOnly("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("org.jetbrains:annotations:23.0.0")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        implementation("org.ow2.asm:${it}:9.2")
    }
}

