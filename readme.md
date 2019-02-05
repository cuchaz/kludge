# Kludge

*Kludge is an idiomatic [Kotlin](https://kotlinlang.org/) API for
[GLFW](https://www.glfw.org/), [Vulkan](https://www.khronos.org/vulkan/),
and [Dear ImGUI](https://github.com/ocornut/imgui).
Focuses on ease-of-use and high performance.*

---

Powered by [LWJGL 3](https://www.lwjgl.org/).

Most likely to be useful for engineering and scientific applications, but also possibly video games.


## Demos

See the [Hello Kludge](https://github.com/cuchaz/hello-kludge) project for demos.


## Progress

Kludge is in a very early stage of development.

The API is **highly** unstable! Expect breaking changes to happen frequently.

Only the a small surface of the underlying APIs have been exposed so far.
Work towards exposing more underlying API features is ongoing.

Dear ImGUI support is working now, with pre-built binaries provided by the
[Kludge-ImGUI](https://github.com/cuchaz/kludge-imgui) project.
Currently, binaries for the following platforms are included with Kludge:

 * Linux x86_64
 
The native code is portable, so other platforms can be supported by simply building
Kludge-ImGUI on those platforms. Contributions welcome!

Other ImGUI wrappers for the JVM aready exist (like [jimgui](https://github.com/ice1000/jimgui)),
but none of the wrappers I tried seemed to easily support the Vulkan backend for ImGUI.


## License

[Three-Clause BSD](license.txt)


## Using

Kludge is in very early development and is not yet included in any artifact repository.

Kludge uses the the [Gradle Build Tool](https://gradle.org/)
and the [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html) to compile sources and manage dependencies.
Until Kludge is added to an artifact repository, applications using Kludge should depend directly on the Kludge source folder by adding

```includeBuild("path/to/kludge")```

to `gradle.settings.kts`, and adding

```
dependencies {
	compile("cuchaz:kludge")
}
```

to `gradle.build.kts`.

Kotlin compiler options may also need to be set in the Gradle build script.
See the [Hello Kludge](https://github.com/cuchaz/hello-kludge/blob/master/build.gradle.kts) project for a complete working example.
