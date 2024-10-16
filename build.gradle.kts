import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.research.code.submissions.clustering.buildutils.configureDetekt
import org.jetbrains.research.code.submissions.clustering.buildutils.configureDiktat
import org.jetbrains.research.code.submissions.clustering.buildutils.createDetektTask
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

    configureDiktat()
    configureDetekt()
}

createDiktatTask()
createDetektTask()
