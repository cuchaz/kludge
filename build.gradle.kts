import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {

	kotlin("jvm") version "1.3.60"

	// https://github.com/Minecrell/licenser
	id("net.minecrell.licenser") version "0.4.1"

	`java-library`
	`maven-publish`
	signing
}

group = "cuchaz"
version = "0.1"

repositories {
	jcenter()
}

dependencies {

	implementation(kotlin("stdlib-jdk8"))

	val linux = "linux"
	val macos = "macos"
	val windows = "windows"
	val all = listOf(linux, macos, windows)

	fun lwjgl(module: String? = null, natives: List<String> = emptyList()) {

		val name = "lwjgl" +
			if (module == null) {
				""
			} else {
				"-$module"
			}
		val lwjglVersion = "3.2.0"

		api("org.lwjgl", name, lwjglVersion)

		for (os in natives) {
			runtimeOnly("org.lwjgl", name, lwjglVersion, classifier = "natives-$os")
		}
	}
	lwjgl(natives=all)
	lwjgl("glfw", natives=all)
	lwjgl("jemalloc", natives=all)
	lwjgl("nfd", natives=all)
	lwjgl("vulkan", natives=listOf(macos))
	
	api("org.joml", "joml", "1.9.19")
	api("net.java.dev.jna:jna:5.5.0")

	testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.1.11")
}

val test by tasks.getting(Test::class) {
	useJUnitPlatform { }
}

java {
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

license {

	include("**/*.kt")
	include("**/*.java")

	header = file("license.header.txt")
}


val sourcesJar = tasks
	.register<Jar>("sourcesJar") {
		classifier = "sources"
		from(sourceSets.main.get().allJava)
	}
	.get()

val javadocJar = tasks
	.register<Jar>("javadocJar") {
		classifier = "javadoc"
		from(tasks.javadoc.get().destinationDir)
	}
	.get()

lateinit var publication: MavenPublication

publishing {

	publications {

		publication = create<MavenPublication>(project.name) {

			// (includes compiled Kotlin classes too)
			from(components["java"])

			artifact(sourcesJar)
			artifact(javadocJar)
		}
	}

	repositories {

		// TODO: add maven repo for maven central, or jcenter

		// local repo for testing
		maven {
			name = "local"
			url = uri("file://$buildDir/repo")
		}
	}
}

signing {
	useGpgCmd()
	sign(publication)
}
