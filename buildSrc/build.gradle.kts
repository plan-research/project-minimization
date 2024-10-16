plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
}
