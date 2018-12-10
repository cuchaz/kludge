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

	fun lwjgl(module: String? = null) {
		val name = "lwjgl" +
			if (module == null) {
				""
			} else {
				"-$module"
			}
		val lwjglVersion = "3.2.0"
		compile("org.lwjgl", name, lwjglVersion)
		for (os in listOf("linux", "macos", "windows")) {
			compile("org.lwjgl", name, lwjglVersion, classifier = "natives-$os")
		}
	}
	lwjgl()
	lwjgl("glfw")
	lwjgl("jemalloc")
	lwjgl("nfd")
	lwjgl("vulkan")

	compile("org.joml", "joml", "1.9.12")

	testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.1.11")
}

val test by tasks.getting(Test::class) {
	useJUnitPlatform { }
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
