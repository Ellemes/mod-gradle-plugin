import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `groovy-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("modPlugin") {
            id = "ellemes.gradle.mod"
            implementationClass = "ellemes.gradle.mod.impl.GradlePlugin"
        }
    }
}

group = "ellemes"
base.archivesName.set("mod-gradle-plugin")
version = "${properties["version"]}"

ext["required_gradle_version"] = gradle.gradleVersion

repositories {
    maven {
        name = "Architectury Maven"
        url = uri("https://maven.architectury.dev")
    }
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
    maven {
        name = "MinecraftForge Maven"
        url = uri("https://maven.minecraftforge.net/")
    }
    maven {
        name = "FabricMC Maven"
        url = uri("https://maven.fabricmc.net")
    }
}

dependencies {
    implementation("dev.architectury:architectury-loom:0.12.0-SNAPSHOT")
    implementation("com.modrinth.minotaur:Minotaur:2.2.0")
    implementation("me.hypherionmc.cursegradle:CurseGradle:2.0.1")
    @Suppress("GradlePackageUpdate")
    implementation("org.jetbrains:annotations:${properties["jetbrains_annotations_version"]}")
    // todo: remove
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        implementation("org.ow2.asm:${it}:9.3")
    }
}

val processSources = tasks.create("processSources", Copy::class) {
    val inputSources = sourceSets["main"].groovy
    val outputSources = project.buildDir.resolve("processedSource")
    inputs.files(inputSources.asFileTree)
    inputs.property("jetbrains_annotations_version", properties["jetbrains_annotations_version"])
    inputs.property("required_gradle_version", properties["required_gradle_version"])
    outputs.dir(outputSources)
    from(inputSources)
    into(outputSources)
    filter<ReplaceTokens>(mapOf("tokens" to mapOf(
            "JETBRAINS_ANNOTATIONS_VERSION" to properties["jetbrains_annotations_version"],
            "REQUIRED_GRADLE_VERSION" to properties["required_gradle_version"]
    )))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = sourceCompatibility
}

tasks.withType(GroovyCompile::class) {
    setSource(processSources.destinationDir)
    dependsOn(processSources)
}
