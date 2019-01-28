package cuchaz.kludge.imgui


class Style internal constructor(
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		Imgui.native.ImGuiStyle_destroy(id)
	}
}

fun Imgui.style() =
	Style(Imgui.native.ImGuiStyle_ImGuiStyle())
