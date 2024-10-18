import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.Coordinates

plugins {
    alias(libs.plugins.intellij)
    kotlin("plugin.serialization") version "1.9.23"
//    alias(libs.plugins.kotlin.serialization)
}

group = rootProject.group
version = rootProject.version

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
/*intellij {
    val pluginName: String by project
    val platformType: String by project
    val platformVersion: String by project
    val platformPlugins: String by project

    this.pluginName.set(pluginName)
    this.version.set(platformVersion)
    this.type.set(platformType)
    this.plugins.set(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))
}*/

intellijPlatform {
    pluginConfiguration {
        val pluginName: String by project
        name = pluginName
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val platformType: String by project
        val platformVersion: String by project
        val platformPlugins: String by project
        val platformBundledPlugins: String by project

        create(platformType, platformVersion)
//        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        bundledPlugins(platformBundledPlugins.split(',').map(String::trim).filter(String::isNotEmpty))

        plugins(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))

        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
        testPlatformDependency(Coordinates("com.jetbrains.intellij.platform", "external-system-test-framework"))
    }
    implementation(project(":project-minimization-core"))
    implementation(libs.kaml)
//    implementation(libs.kotlinx.coroutines.core)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.1.0")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.1.0")
    testCompileOnly("junit:junit:4.13.1")
}
tasks {
    val test by getting(Test::class) {
        isScanForTestClasses = false
        // Only run tests from classes that end with "Test"
        include("**/*Test.class")
        systemProperty("idea.is.internal", true)
    }
}