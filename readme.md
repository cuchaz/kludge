# Kludge

*Kludge is a thin wrapper for [GLFW](https://www.glfw.org/) and [Vulkan](https://www.khronos.org/vulkan/)
that presents an idiomatic [Kotlin](https://kotlinlang.org/) API focusing on ease-of-use without overly penalizing performance.*

---

Powered by [LWJGL 3](https://www.lwjgl.org/).

Most likely to be useful for engineering and scientific applications, but also possibly video games.


## Demos

 * [Hello World Triangle](https://github.com/cuchaz/hello-kludge/blob/master/src/main/kotlin/cuchaz/hellokludge/helloworld/main.kt): A complete Vulkan demo in about 200 lines of Kotlin code.


## Progress

Kludge is in an extremely early stage of development.

Only the bare minimum of the Vulkan API has been exposed to support the hello world triangle demo.
More work is needed to support applications requiring features beyond the bare minimum.

In the near future, Kludge will expose an idiomatic API for [Dear ImGUI](https://github.com/ocornut/imgui)
(via [jimgui](https://github.com/ice1000/jimgui)) as well, but work on this has not yet begun.

In the less-near future, Kluge may include a higher-level rendering engine based on its own Vulkan wrapper.


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
