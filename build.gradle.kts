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
        implementation(libs.arrow.core)
        testImplementation(kotlin("test"))
    }

    tasks {
        // Set the JVM compatibility versions
        withType<JavaCompile> {
            sourceCompatibility = "21"
            targetCompatibility = "21"
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "21"
        }

        test {
            useJUnitPlatform()
        }
    }

    kotlin {
        jvmToolchain(21)
    }
}
