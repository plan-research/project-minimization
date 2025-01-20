import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.Coordinates
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    alias(libs.plugins.intellij)
    alias(libs.plugins.serialization)
}

group = rootProject.group
version = rootProject.version

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
        bundledPlugins(platformBundledPlugins.split(',').map(String::trim).filter(String::isNotEmpty))

        plugins(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
        testPlatformDependency(Coordinates("com.jetbrains.intellij.platform", "external-system-test-framework"))
    }
    implementation(project(":project-minimization-core"))
    implementation(libs.kotlinx.immutable)
    implementation(libs.kotlinx.serialization)
    implementation(libs.graphviz.java)
    implementation(libs.graphviz.kotlin)

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

tasks.named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}

tasks.test {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}