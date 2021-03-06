/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class Semaphore internal constructor(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	override fun toString() = "0x%x".format(id)

	override fun close() {
		vkDestroySemaphore(device.vkDevice, id, null)
	}
}

fun Device.semaphore(): Semaphore {
	memstack { mem ->
		val info = VkSemaphoreCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
		val pSemaphore = mem.mallocLong(1)
		vkCreateSemaphore(vkDevice, info, null, pSemaphore)
			.orFail("failed to create semaphore")
		return Semaphore(this, pSemaphore.get(0))
	}
}
