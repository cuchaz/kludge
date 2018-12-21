/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import cuchaz.kludge.window.Window
import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.vulkan.KHRSurface.*


fun Vulkan.surface(win: Window): Surface {
	memstack { mem ->
		val pSurf = mem.mallocLong(1)
		glfwCreateWindowSurface(instance, win.id, null, pSurf)
		return Surface(this, pSurf.get(0))
	}
}

class Surface internal constructor(
	internal val vulkan: Vulkan,
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		vkDestroySurfaceKHR(vulkan.instance, id, null)
	}
}

// TODO: off-screen surface?
