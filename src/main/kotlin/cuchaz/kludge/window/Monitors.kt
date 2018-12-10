/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import org.lwjgl.glfw.GLFW.*


object Monitors {

	// TODO: enumerate monitors somehow?

	val primary = Monitor(glfwGetPrimaryMonitor())
}
