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

		vulkan.debugMessager(
			desiredSeverities = IntFlags.of(
				DebugMessager.Severity.Error,
				DebugMessager.Severity.Warning,
				DebugMessager.Severity.Verbose
			)
		) { severityFlags, typeFlags, msg ->
			println("VULKAN: $msg")
		}.use { debug ->

			vulkan.debugInfo("Debug message!")

			// print device info
			for (device in vulkan.physicalDevices) {
				println("device: $device")
				println("extensions: ${device.extensionNames}")
				device.queueFamilies.forEach { println("\t$it") }
			}

			Window(
				size = Size(640, 480),
				title = "Kludge Demo"
			).use { win ->

				win.centerOn(Monitors.primary)
				win.visible = true

				// make a surface
				vulkan.surface(win).use { surface ->

					// connect to a device
					val physicalDevice = vulkan.physicalDevices[0]
					val graphicsFamily = physicalDevice.findQueueFamily(IntFlags.of(PhysicalDevice.QueueFamily.Flags.Graphics))
					val surfaceFamily = physicalDevice.findQueueFamily(surface)
					physicalDevice.device(
						queuePriorities = mapOf(
							graphicsFamily to listOf(1.0f),
							surfaceFamily to listOf(1.0f)
						),
						extensionNames = setOf(PhysicalDevice.SwapchainExtension)
					).use { device ->

						println("have a device!: $device")

						val graphicsQueue = device.queues[graphicsFamily]!![0]
						val surfaceQueue = device.queues[surfaceFamily]!![0]
						println("have queues:\n\t$graphicsQueue\n\t$surfaceQueue")
					}

					// look for swapchain support
					physicalDevice.swapchainSupport(surface).apply {
						println(capabilities)
						println("surface format: $surfaceFormats")
						println("present mode: $presentModes")
					}

					// main loop
					while (!win.shouldClose()) {

						// TODO: render something

						Windows.pollEvents()
					}
				}
			}
		}
	}

	// cleanup
	Windows.close()
}
