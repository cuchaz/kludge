package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class Framebuffer internal constructor(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		vkDestroyFramebuffer(device.vkDevice, id, null)
	}
}

fun Device.framebuffer(
	graphicsPipeline: GraphicsPipeline,
	imageViews: List<Image.View>,
	width: Int,
	height: Int,
	layers: Int
) : Framebuffer {
	memstack { mem ->

		val info = VkFramebufferCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
			.renderPass(graphicsPipeline.renderPassId)
			.pAttachments(imageViews.map { it.id }.toBuffer(mem))
			.width(width)
			.height(height)
			.layers(layers)

		val pFramebuffer = mem.mallocLong(1)
		vkCreateFramebuffer(vkDevice, info, null, pFramebuffer)
			.orFail("failed to create framebuffer")
		return Framebuffer(this, pFramebuffer.get(0))
	}
}
