plugins {
    alias(libs.plugins.intellij)
}

group = rootProject.group
version = rootProject.version

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    val pluginName: String by project
    val platformType: String by project
    val platformVersion: String by project
    val platformPlugins: String by project

    this.pluginName.set(pluginName)
    this.version.set(platformVersion)
    this.type.set(platformType)
    this.plugins.set(platformPlugins.split(',').map(String::trim).filter(String::isNotEmpty))
}

dependencies {
    implementation(project(":project-minimization-core"))
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

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
    }
}