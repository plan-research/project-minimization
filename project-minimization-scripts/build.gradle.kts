plugins {
    id("java")
}

group = "org.plan.research.minimization"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":project-minimization-plugin"))
    implementation(libs.kaml)
    implementation(libs.csv)
}

tasks.test {
    useJUnitPlatform()
}