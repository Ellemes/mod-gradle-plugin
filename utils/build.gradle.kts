plugins {
    `maven-publish`
}

dependencies {
    implementation("org.simpleframework:simple-xml:2.7.1")

    listOf("asm-util", "asm-tree", "asm-commons", "asm-analysis", "asm").forEach {
        implementation("org.ow2.asm:${it}:9.2")
    }

    implementation(gradleApi())
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))
        }
    }
}