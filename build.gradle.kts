import org.gradle.configurationcache.extensions.capitalized

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
    }

    group = "ninjaphenix.gradle.mod"
    version = "6.2.2.0"

    if (project.name != "utils") {
        apply(plugin = "java-gradle-plugin")

        val niceName = project.name.capitalized()

        gradlePlugin {
            plugins {
                create("mod" + niceName + "Plugin") {
                    id = "ninjaphenix.gradle.mod.${project.name}"
                    implementationClass = "ninjaphenix.gradle.mod.${project.name}.impl.Main"
                }
            }
        }

        base.archivesName.set("mod-gradle-${project.name}-plugin")

        repositories {
            maven {
                name = "Architectury Maven"
                url = uri("https://maven.architectury.dev")
            }
        }

        dependencies {
            implementation("architectury-plugin:architectury-plugin.gradle.plugin:${properties["arch_plugin_version"]}")
            implementation(project(":utils"))
        }
    }

    dependencies {
        implementation("org.jetbrains:annotations:${properties["jetbrains_annotations_version"]}")
    }
}

tasks.create("publishPlugin") {
    for (subproject in subprojects) {
        this.dependsOn(subproject.tasks.getByName("publishToMavenLocal"))
    }
}



//repositories {
//    maven {
//        name = "Architectury Maven"
//        url = uri("https://maven.architectury.dev")
//    }
//    mavenCentral()
//}
//
//dependencies {
//    compileOnly("dev.architectury:architectury-loom:0.11.0-SNAPSHOT")
//    compileOnly("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
//    implementation("org.jetbrains:annotations:23.0.0")
//    implementation("org.simpleframework:simple-xml:2.7.1")
//
//    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
//        implementation("org.ow2.asm:${it}:9.2")
//    }
//}

