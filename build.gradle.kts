import net.minecrell.gradle.licenser.LicenseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {

	kotlin("jvm") version "1.3.20-eap-25"

	// https://github.com/Minecrell/licenser
	id("net.minecrell.licenser") version "0.4.1"
}

group = "cuchaz"
version = "0.1"

repositories {
	maven("http://dl.bintray.com/kotlin/kotlin-eap")
	jcenter()
}

dependencies {

	compile(kotlin("stdlib-jdk8"))

	fun lwjgl(module: String? = null, natives: Boolean = false) {

		val name = "lwjgl" +
			if (module == null) {
				""
			} else {
				"-$module"
			}
		val lwjglVersion = "3.2.0"

		compile("org.lwjgl", name, lwjglVersion)

		if (natives) {
			for (os in listOf("linux", "macos", "windows")) {
				runtimeOnly("org.lwjgl", name, lwjglVersion, classifier = "natives-$os")
			}
		}
	}
	lwjgl(natives=true)
	lwjgl("glfw", natives=true)
	lwjgl("jemalloc", natives=true)
	lwjgl("nfd", natives=true)
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

	kotlinOptions {

		jvmTarget = "1.8"

		// enable experimental features
		languageVersion = "1.3"
		freeCompilerArgs += "-XXLanguage:+InlineClasses"
	}
}

(extensions["license"] as LicenseExtension).apply {

	include("**/*.kt")
	include("**/*.java")

	header = file("license.header.txt")
}
