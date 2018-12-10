/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import org.lwjgl.glfw.GLFW.*


fun Boolean.toGLFW() =
	if (this) {
		GLFW_TRUE
	} else {
		GLFW_FALSE
	}

data class Pos(val x: Int, val y: Int)
data class Size(val width: Int, val height: Int)
