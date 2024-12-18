group = rootProject.group
version = rootProject.version

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.jqwik)
    testImplementation(libs.jqwik.kotlin)

    implementation(libs.kotlinx.immutable)
    implementation(libs.graphviz.java)
    implementation(libs.graphviz.kotlin)
}
