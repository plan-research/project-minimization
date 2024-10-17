plugins {
    kotlin("jvm") version "1.9.20"
}

group = "com.jetbrains.research.plan"
version = "1.0-SNAPSHOT"
sourceSets {
}
repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}