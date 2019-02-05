/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

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
