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
}
