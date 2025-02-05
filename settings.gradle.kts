rootProject.name = "project-minimization"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

include(
    "project-minimization-core",
    "project-minimization-plugin",
    "project-minimization-scripts",
)
