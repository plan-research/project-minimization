import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.plan.research.minimization"
version = "1.0-SNAPSHOT"

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
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
