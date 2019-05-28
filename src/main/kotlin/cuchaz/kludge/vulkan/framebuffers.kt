/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class Framebuffer internal constructor(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	override fun toString() = "0x%x".format(id)

	override fun close() {
		vkDestroyFramebuffer(device.vkDevice, id, null)
	}
}

fun Device.framebuffer(
	renderPass: RenderPass,
	imageViews: List<Image.View>,
	extent: Extent2D,
	layers: Int = 1
) : Framebuffer {
	memstack { mem ->

		val info = VkFramebufferCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
			.renderPass(renderPass.id)
			.pAttachments(imageViews.map { it.id }.toBuffer(mem))
			.width(extent.width)
			.height(extent.height)
			.layers(layers)

		val pFramebuffer = mem.mallocLong(1)
		vkCreateFramebuffer(vkDevice, info, null, pFramebuffer)
			.orFail("failed to create framebuffer")
		return Framebuffer(this, pFramebuffer.get(0))
	}
}
