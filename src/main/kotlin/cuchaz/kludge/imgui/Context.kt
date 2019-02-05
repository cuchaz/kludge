/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.imgui


class Context internal constructor(
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		Imgui.native.igDestroyContext(id)
	}
}

fun Imgui.context(fontAtlas: FontAtlas? = null) =
	Context(Imgui.native.igCreateContext(fontAtlas?.id ?: 0))
