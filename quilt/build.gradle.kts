repositories {
    maven {
        name = "Quilt Snapshot Maven"
        url = uri("https://maven.quiltmc.org/repository/snapshot/")
    }
}

dependencies {
    implementation("org.quiltmc:loom:${properties["quilt_loom_version"]}")
}