plugins {
	id("org.jetbrains.kotlin.jvm") version "2.0.20"
}

repositories {
	mavenCentral()
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
	implementation("org.assertj:assertj-core:3.26.3")
	implementation("org.seleniumhq.selenium:selenium-api:4.24.0")
}
