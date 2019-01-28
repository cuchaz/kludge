package cuchaz.kludge.imgui


class FontAtlas internal constructor(
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		Imgui.native.ImFontAtlas_destroy(id)
	}
}

fun Imgui.fontAtlas(
) = FontAtlas(Imgui.native.ImFontAtlas_ImFontAtlas())
