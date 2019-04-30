/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import org.lwjgl.glfw.GLFW.*


object Monitors {

	val primary get() = Monitor(glfwGetPrimaryMonitor())

	val all: List<Monitor> get() =
		ArrayList<Monitor>().apply {
			val ps = glfwGetMonitors() ?: return@apply
			while (ps.hasRemaining()) {
				add(Monitor(ps.get()))
			}
		}

	/**
	 * Of all monitors that can fit the given size, perfer the primary monitor.
	 * Otherwise, prefer the first non-primary monitor that fist.
	 * If no monitors can fit the size, use the primary monitor anyway.
	 */
	fun findBest(size: Size): Monitor {

		// what are all the monitors that could fit the size?
		val possibleMonitors = all.filter {
			size.width <= it.size.width && size.height <= it.size.height
		}

		// if the primary monitor is there, pick that
		possibleMonitors.find { it == primary }?.let { return it }

		// otherwise, pick the first monitor
		possibleMonitors.firstOrNull()?.let { return it }

		// finally, fallback to the primary monitor
		return primary
	}
}
