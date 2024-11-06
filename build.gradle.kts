import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.research.code.submissions.clustering.buildutils.configureDiktat
import org.jetbrains.research.code.submissions.clustering.buildutils.createDiktatTask

group = "org.plan.research.minimization"
version = "1.0-SNAPSHOT"

plugins {
    java
    kotlin
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
        implementation(libs.kotlin.logging)
        implementation(libs.logback.classic)

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
            useJUnitPlatform {
                // Capture standard and error output from failed tests
                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                    events("failed")
                    showStandardStreams = true
                }
            }
        }
    }

    kotlin {
        jvmToolchain(21)
    }

    configureDiktat()
}

createDiktatTask()
