pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "mod-gradle-plugin"

include("utils")
include("root")
include("common")
include("fabric")
include("quilt")
include("forge")