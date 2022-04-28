import org.apache.tools.ant.filters.ReplaceTokens

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
version = "${properties["version"]}+${properties["minecraft_version"]}"

repositories {
    maven {
        name = "Architectury Maven"
        url = uri("https://maven.architectury.dev")
    }
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("dev.architectury:architectury-loom:0.11.0.9999")
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("org.jetbrains:annotations:${properties["jetbrains_annotations_version"]}")
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        implementation("org.ow2.asm:${it}:9.2")
    }
}

val processSources = tasks.create("processSources", Copy::class) {
    val inputSources = sourceSets.getByName("main").allJava
    val outputSources = project.buildDir.resolve("processedSource")
    inputs.files(inputSources.asFileTree)
    outputs.dir(outputSources)
    from(inputSources)
    into(outputSources)
    filter<ReplaceTokens>(mapOf("tokens" to mapOf(
            "MINECRAFT_VERSION" to properties["minecraft_version"],
            "JETBRAINS_ANNOTATIONS_VERSION" to properties["jetbrains_annotations_version"],
            "REQUIRED_GRADLE_VERSION" to properties["required_gradle_version"]
    )))
}

tasks.withType(JavaCompile::class) {
    setSource(processSources.destinationDir)
    dependsOn(processSources)
}