import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.research.code.submissions.clustering.buildutils.configureDiktat
import org.jetbrains.research.code.submissions.clustering.buildutils.createDiktatTask

group = "org.plan.research.minimization"
version = "1.0-SNAPSHOT"

plugins {
    java
    kotlin
    alias(libs.plugins.ksp)
}

allprojects {
    val libs = rootProject.libs

    apply {
        apply {
            plugin("java")
            plugin("kotlin")
            plugin(libs.plugins.ksp.get().pluginId)
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(libs.arrow.core)
        implementation(libs.kotlin.logging)
        implementation(libs.logback.classic)
        implementation(libs.arrow.optics)
        ksp(libs.arrow.ksp.plugin)

        testImplementation(kotlin("test"))
    }

    tasks {
        // Set the JVM compatibility versions
        withType<JavaCompile> {
            sourceCompatibility = "21"
            targetCompatibility = "21"
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
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            javaParameters = true
            freeCompilerArgs = listOf(
                "-Xnullability-annotations=@org.jspecify.annotations:strict",
                "-Xemit-jvm-type-annotations",
                "-Xcontext-receivers"
            )
        }
    }

    configureDiktat()
}

createDiktatTask()

tasks.named("diktatFix") {
    notCompatibleWithConfigurationCache("https://github.com/saveourtool/diktat/issues/1732")
}