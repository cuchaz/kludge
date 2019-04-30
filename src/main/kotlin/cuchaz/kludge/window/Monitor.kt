/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import cuchaz.kludge.tools.memstack
import org.lwjgl.glfw.GLFW.*


class Monitor(private val id: Long) {

	val videoMode get() = glfwGetVideoMode(id)
		?: throw NoSuchElementException("failed to get video mode")

	val pos: Pos get() {
		memstack { mem ->
			val x = mem.mallocInt(1)
			val y = mem.mallocInt(1)
			glfwGetMonitorPos(id, x, y)
			return Pos(x.get(0), y.get(0))
		}
	}

	val size: Size get() = videoMode.let { Size(it.width(), it.height()) }

	override fun hashCode() = id.hashCode()
	override fun equals(other: Any?) = other is Monitor && other.id == this.id
}
