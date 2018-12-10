/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*


class Window(
	val resizable: Boolean = true,
	title: String = "Windnow",
	size: Size = Size(300, 300)
): AutoCloseable {

	private val id: Long

	init {

		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
		glfwWindowHint(GLFW_RESIZABLE, resizable.toGLFW())

		id = glfwCreateWindow(size.width, size.height, title, NULL, NULL)
		if (id == NULL) {
			throw RuntimeException("failed to create GLFW window")
		}

		// TODO: listen to keyboard, mouse
	}

	fun shouldClose() = glfwWindowShouldClose(id)

	override fun close() {
		glfwFreeCallbacks(id)
		glfwDestroyWindow(id)
	}

	var title: String = title
		set(value) {
			glfwSetWindowTitle(id, value)
			field = title
		}

	var visible: Boolean = false
		set(value) {
			if (value) {
				glfwShowWindow(id)
			} else {
				glfwHideWindow(id)
			}
			field = value
		}

	var pos: Pos
		get() {
			MemoryStack.stackPush().use { stack ->
				val x = stack.mallocInt(1)
				val y = stack.mallocInt(1)
				glfwGetWindowPos(id, x, y)
				return Pos(x.get(0), y.get(0))
			}
		}
		set(value) {
			glfwSetWindowPos(id, value.x, value.y)
		}

	var size: Size
		get() {
			MemoryStack.stackPush().use { stack ->
				val width = stack.mallocInt(1)
				val height = stack.mallocInt(1)
				glfwGetWindowSize(id, width, height)
				return Size(width.get(0), height.get(0))
			}
		}
		set(value) {
			glfwSetWindowSize(id, value.width, value.height)
		}

	fun centerOn(monitor: Monitor) {
		val monitorSize = monitor.size
		val windowSize = size
		pos = Pos(
			(monitorSize.width - windowSize.width)/2,
			(monitorSize.height - windowSize.height)/2
		)
	}
}
