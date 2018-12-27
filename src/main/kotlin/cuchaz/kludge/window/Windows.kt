/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import cuchaz.kludge.tools.toStrings
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVulkan.*
import java.io.Closeable
import java.io.PrintStream


object Windows : AutoCloseable {

	fun init() {

		// init GLFW
		if (!glfwInit()) {
			throw IllegalStateException("GLFW init failed")
		}

		// configure GLFW
		glfwDefaultWindowHints()
	}

	override fun close() {
		glfwTerminate()
		errors.close()
	}

	val isVulkanSupported: Boolean by lazy {
		glfwVulkanSupported()
	}

	val requiredVulkanExtensions: Set<String> by lazy {
		glfwGetRequiredInstanceExtensions()
			?.toStrings()
			?.toMutableSet()
			?: throw Error("Can't get GLFW extensions")
	}

	fun pollEvents() {
		glfwPollEvents()
	}

	object errors : Closeable {

		fun setOut(value: PrintStream) {
			GLFWErrorCallback.createPrint(value).set()
		}

		override fun close() {
			glfwSetErrorCallback(null)?.free()
		}
	}
}
