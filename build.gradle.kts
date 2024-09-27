import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.plan.research.minimization"
version = "1.0-SNAPSHOT"

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    val libs = rootProject.libs

    apply {
        apply {
            plugin("java")
            plugin("kotlin")
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // these libraries are already provided inside the IDEA plugin
        if (project.name != "project-minimization-plugin") {
            implementation(kotlin("stdlib-jdk8"))
            implementation(libs.kotlinx.coroutines.core)
        }
        implementation(libs.arrow.core)

        testImplementation(kotlin("test"))
    }

    tasks {
        // Set the JVM compatibility versions
        withType<JavaCompile> {
            sourceCompatibility = "17"
            targetCompatibility = "17"
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }

        test {
            useJUnitPlatform()
        }
    }

    kotlin {
        jvmToolchain(17)
    }
}
