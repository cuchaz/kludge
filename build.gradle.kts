import net.minecrell.gradle.licenser.LicenseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {

    java
    kotlin("jvm") version "1.3.10"

	// https://github.com/Minecrell/licenser
	id("net.minecrell.licenser") version "0.4.1"
}

group = "cuchaz"
version = "0.1"

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

(extensions["license"] as LicenseExtension).apply {

	include("**/*.kt")
	include("**/*.java")

	header = file("license.header.txt")
}
