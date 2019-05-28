/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import org.lwjgl.vulkan.VK10.*


abstract class Pipeline(
	val device: Device,
	val bindPoint: PipelineBindPoint,
	internal val id: Long,
	internal val layoutId: Long
) : AutoCloseable {

	override fun toString() = "0x%x".format(id)

	override fun close() {
		vkDestroyPipelineLayout(device.vkDevice, layoutId, null)
		vkDestroyPipeline(device.vkDevice, id, null)
	}
}
