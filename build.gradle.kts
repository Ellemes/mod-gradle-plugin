plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("modPlugin") {
            id = "ninjaphenix.gradle.mod"
            implementationClass = "ninjaphenix.gradle.mod.GradlePlugin"
        }
    }
}

repositories {
    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net")
    }
}

dependencies {
    compileOnly("net.fabricmc:fabric-loom:0.11.29")
}

