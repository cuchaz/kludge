/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import org.lwjgl.glfw.GLFW.*


class Monitor(private val id: Long) {

	val videoMode get() = glfwGetVideoMode(id)
		?: throw NoSuchElementException("failed to get video mode")

	val size get() = videoMode.let { Size(it.width(), it.height()) }
}
