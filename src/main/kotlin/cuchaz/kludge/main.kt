/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.vulkan.*
import cuchaz.kludge.window.*


fun main(args: Array<String>) {

	// listen to GLFW error messages
	Windows.init()
	Windows.errors.setOut(System.err)

	// check for vulkan support
	if (!Windows.isVulkanSupported) {
		throw Error("No Vulkan support from GLFW")
	}

	Vulkan(
		extensionNames = Windows.requiredVulkanExtensions + setOf(Vulkan.DebugExtension),
		layerNames = setOf(Vulkan.StandardValidationLayer)
	).use { vulkan ->

		vulkan.debugMessager { severityFlags, typeFlags, msg ->
			println("VULKAN: $msg")
		}.use { debug ->

			vulkan.debugInfo("Debug message!")

			// print device info
			for (device in vulkan.physicalDevices) {
				println("device: $device")
				device.queueFamilies.forEach { println("\t$it") }
			}

			// connect to a device
			vulkan.physicalDevices[0].let { pd ->
				pd.device(
					queuePriorities = mapOf(
						pd.findQueueFamily(IntFlags.of(PhysicalDevice.QueueFamily.Flags.Graphics)) to listOf(1.0f)
					)
				)
			}.use { device ->
				println("have a device!: $device")
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
