import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties


plugins {

	kotlin("jvm") version "1.3.60"

	// https://github.com/Minecrell/licenser
	id("net.minecrell.licenser") version "0.4.1"

	`java-library`
	`maven-publish`
	signing
}

// read secret properties, if any
properties["secrets.path"]
	?.let { Paths.get(it as String) }
	?.takeIf { Files.exists(it) }
	?.let { path ->
		val props = path.toFile().inputStream().use { input ->
			Properties().apply {
				load(input)
			}
		}
		for (name in props.stringPropertyNames()) {
			ext.set(name, props.getProperty(name))
		}
	}

fun getSecret(name: String) =
	findProperty(name) as? String
		?: throw NoSuchElementException("No value defined for secret: $name")


group = "com.cuchazinteractive"
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
		val lwjglVersion = "3.2.2"

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

			pom {

				name.set(project.name)
				description.set("Kludge is an idiomatic Kotlin API for GLFW, Vulkan, and Dear ImGUI. Focuses on ease-of-use and high performance.")
				url.set("https://github.com/cuchaz/kludge")

				licenses {
					license {
						name.set("BSD-3-Clause License")
						url.set("https://opensource.org/licenses/BSD-3-Clause")
					}
				}

				developers {
					developer {
						id.set("cuchaz")
						name.set("Jeff Martin")
						email.set("jeff@cuchazinteractive.com")
						organization.set("Cuchaz Interactive, LLC")
					}
				}

				scm {
					connection.set("scm:git:git://github.com/cuchaz/kludge.git")
					developerConnection.set("scm:git:git://github.com/cuchaz/kludge.git")
					url.set("https://github.com/cuchaz/kludge/tree/master")
				}
			}
		}
	}

	repositories {

		// OSSRH for Maven Central
		// instructions at: https://central.sonatype.org/pages/ossrh-guide.html
		// manage staging/releases online at: https://oss.sonatype.org/
		maven {
			name = "OSSRH"
			url = if (version.toString().endsWith("-SNAPSHOT")) {
				uri("https://oss.sonatype.org/content/repositories/snapshots")
			} else {
				uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
			}

			credentials {
				username = getSecret("sonatype.login")
				password = getSecret("sonatype.password")
			}
		}

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
