/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge

import cuchaz.kludge.vulkan.*
import cuchaz.kludge.window.*


fun main(args: Array<String>) {

	// listen to GLFW error messages
	Windows.init()
	Windows.errors.setOut(System.err)

	// check for vulkan support
	if (!Windows.isVulkanSupported) {
		throw Error("No Vulkan support")
	}

	Vulkan(
		extensionNames = Windows.requiredVulkanExtensions + setOf(Vulkan.DebugExtension),
		layerNames = setOf(Vulkan.StandardValidationLayer)
	).use { vulkan ->

		vulkan.debugMessager { severityFlags, typeFlags, msg ->
			println("VULKAN: $msg")
		}.use {

			vulkan.debugInfo("Debug message!")

			// TEMP: list devices
			for (device in vulkan.physicalDevices) {
				println("device: $device")
			}

			/* TEMP
			Window(
				size = Size(640, 480),
				title = "Kludge Demo"
			).use { win ->

				win.centerOn(Monitors.primary)
				win.visible = true

				// main loop
				while (!win.shouldClose()) {

					// TODO: render something

					Windows.pollEvents()
				}
			}
			*/
		}
	}

	// cleanup
	Windows.close()
}
