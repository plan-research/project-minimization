group = rootProject.group
version = rootProject.version

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.kotlinx.coroutines.core)
}
