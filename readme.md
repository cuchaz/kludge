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

Kludge is in a medium-to-early stage of development.

The API is becoming more and more stable, but expect breaking changes to happen occasionally.

Only the mostly commonly-used surface of the underlying APIs have been exposed so far.
Work towards exposing more underlying API features is ongoing and mainly driven by
the couple of applications that depend on Kludge.

Dear ImGUI support is working now, with pre-built binaries provided by the
[Kludge-ImGUI](https://github.com/cuchaz/kludge-imgui) project.
Currently, binaries for the following platforms are included with Kludge:

 * Linux x86_64
 * Windows x86_64
 * Mac OSX

The native code is portable, so other platforms can be supported by simply building
Kludge-ImGUI on those platforms. Contributions welcome!

Other ImGUI wrappers for the JVM aready exist (like [jimgui](https://github.com/ice1000/jimgui)),
but none of the wrappers I tried seemed to easily support the Vulkan backend for ImGUI.


## License

[Three-Clause BSD](license.txt)


## Using

Kludge is available on Maven Central. Add it to your [Gradle](https://gradle.org/) `gradle.build.kts` with:

```
dependencies {
    implementation("com.cuchazinteractive:kludge:0.1")
}
```
