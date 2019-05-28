/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.indexOfOrNull
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK11.*


class RenderPass internal constructor(
	val device: Device,
	internal val id: Long,
	val attachments: List<Attachment>,
	val subpasses: List<Subpass>,
	val subpassDependencies: List<SubpassDependency>
) : AutoCloseable {

	override fun toString() = "0x%x".format(id)

	override fun close() {
		vkDestroyRenderPass(device.vkDevice, id, null)
	}
}

fun Device.renderPass(
	attachments: List<Attachment>,
	subpasses: List<Subpass>,
	subpassDependencies: List<SubpassDependency>
): RenderPass {
	memstack { mem ->

		// build the attachments
		val pAttachments = VkAttachmentDescription.callocStack(attachments.size, mem)
		for (attachment in attachments) {
			pAttachments.get()
				.format(attachment.format.ordinal)
				.samples(attachment.samples.value)
				.loadOp(attachment.loadOp.ordinal)
				.storeOp(attachment.storeOp.ordinal)
				.stencilLoadOp(attachment.stencilLoadOp.ordinal)
				.stencilStoreOp(attachment.stencilStoreOp.ordinal)
				.initialLayout(attachment.initialLayout.value)
				.finalLayout(attachment.finalLayout.value)
		}
		pAttachments.flip()

		fun Pair<Attachment,Image.Layout>.toRef(): Attachment.Ref {
			val (attachment, layout) = this
			val index = attachments.indexOfOrNull(attachment)
				?: throw NoSuchElementException("attachment is not in attachments list: $attachment")
			return attachment.Ref(index, layout)
		}

		// build the subpasses
		val pSubpasses = VkSubpassDescription.callocStack(subpasses.size, mem)
		for (subpass in subpasses) {
			pSubpasses.get()
				.pipelineBindPoint(subpass.pipelineBindPoint.ordinal)
				.pInputAttachments(subpass.inputAttachments.map { it.toRef() }.toBuffer(mem))
				.pColorAttachments(subpass.colorAttachments.map { it.toRef() }.toBuffer(mem))
				.colorAttachmentCount(subpass.colorAttachments.size)
				.pResolveAttachments(subpass.resolveAttachments.map { it.toRef() }.toBuffer(mem))
				.pDepthStencilAttachment(subpass.depthStencilAttachment?.toRef()?.toVulkan(mem))
				.pPreserveAttachments(subpass.preserveAttachments.toBuffer(mem))
		}
		pSubpasses.flip()

		fun getSubpassIndex(subpass: Subpass?) =
			if (subpass === Subpass.External) {
				VK_SUBPASS_EXTERNAL
			} else {
				subpasses.indexOfOrNull(subpass) ?: throw NoSuchElementException("subpass not in subpasses list: $subpass")
			}

		// build the subpass dependencies
		val pSubpassDependencies = VkSubpassDependency.callocStack(subpassDependencies.size, mem)
		for (subpassDependency in subpassDependencies) {
			pSubpassDependencies.get()
				.srcSubpass(getSubpassIndex(subpassDependency.src.subpass))
				.dstSubpass(getSubpassIndex(subpassDependency.dst.subpass))
				.srcStageMask(subpassDependency.src.stageMask.value)
				.dstStageMask(subpassDependency.dst.stageMask.value)
				.srcAccessMask(subpassDependency.src.accessMask.value)
				.dstAccessMask(subpassDependency.dst.accessMask.value)
				.dependencyFlags(subpassDependency.dependencyFlags.value)
		}
		pSubpassDependencies.flip()

		// finally, make the render pass
		val pRenderPassInfo = VkRenderPassCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
			.pAttachments(pAttachments)
			.pSubpasses(pSubpasses)
			.pDependencies(pSubpassDependencies)
		val pRenderPass = mem.mallocLong(1)
		vkCreateRenderPass(vkDevice, pRenderPassInfo, null, pRenderPass)
			.orFail("failed to create render pass")

		return RenderPass(this, pRenderPass.get(0), attachments, subpasses, subpassDependencies)
	}
}

enum class LoadOp {
	Load,
	Clear,
	DontCare
}

enum class StoreOp {
	Store,
	DontCare
}

data class Attachment(
	val format: Image.Format,
	val samples: SampleCount = SampleCount.One,
	val loadOp: LoadOp = LoadOp.DontCare,
	val storeOp: StoreOp = StoreOp.DontCare,
	val stencilLoadOp: LoadOp = LoadOp.DontCare,
	val stencilStoreOp: StoreOp = StoreOp.DontCare,
	val initialLayout: Image.Layout = Image.Layout.Undefined,
	val finalLayout: Image.Layout
) {
	internal inner class Ref(
		val index: Int,
		val layout: Image.Layout
	) {
		val attachment: Attachment get() = this@Attachment

		internal fun toVulkan(mem: MemoryStack) = VkAttachmentReference.callocStack(mem).set(this)
	}
}

internal fun VkAttachmentReference.set(ref: Attachment.Ref) =
	apply {
		attachment(ref.index)
		layout(ref.layout.ordinal)
	}
internal fun Collection<Attachment.Ref>.toBuffer(mem: MemoryStack) =
	if (isEmpty()) {
		null
	} else {
		VkAttachmentReference.callocStack(size, mem).apply {
			for (ref in this@toBuffer) {
				get().set(ref)
			}
			flip()
		}
	}

data class Subpass(
	val pipelineBindPoint: PipelineBindPoint,
	val inputAttachments: List<Pair<Attachment,Image.Layout>> = emptyList(),
	val colorAttachments: List<Pair<Attachment,Image.Layout>> = emptyList(),
	val resolveAttachments: List<Pair<Attachment,Image.Layout>> = emptyList(),
	val depthStencilAttachment: Pair<Attachment,Image.Layout>? = null,
	val preserveAttachments: List<Int> = emptyList() // TODO: hide these indices somehow?
) {

	inner class Ref(
		val index: Int
	) {
		val subpass: Subpass get() = this@Subpass
	}

	companion object {
		val External: Subpass? = null
	}
}

enum class Dependency(override val value: Int) : IntFlags.Bit {
	ByRegion(VK_DEPENDENCY_BY_REGION_BIT),
	ViewLocal(VK_DEPENDENCY_VIEW_LOCAL_BIT),
	DeviceGroup(VK_DEPENDENCY_DEVICE_GROUP_BIT)
}

data class SubpassDependency(
	val src: Part,
	val dst: Part,
	val dependencyFlags: IntFlags<Dependency> = IntFlags(0)
) {

	data class Part internal constructor(
		val subpass: Subpass?,
		val stageMask: IntFlags<PipelineStage>,
		val accessMask: IntFlags<Access>
	)
}

fun Subpass?.dependency(
	stage: IntFlags<PipelineStage>,
	access: IntFlags<Access> = IntFlags(0)
) =
	SubpassDependency.Part(this, stage, access)

enum class Access(override val value: Int) : IntFlags.Bit {
	IndirectCommandRead(VK_ACCESS_INDIRECT_COMMAND_READ_BIT),
	IndexRead(VK_ACCESS_INDEX_READ_BIT),
	VertexAttributeRead(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT),
	UniformRead(VK_ACCESS_UNIFORM_READ_BIT),
	InputAttachmentRead(VK_ACCESS_INPUT_ATTACHMENT_READ_BIT),
	ShaderRead(VK_ACCESS_SHADER_READ_BIT),
	ShaderWrite(VK_ACCESS_SHADER_WRITE_BIT),
	ColorAttachmentRead(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT),
	ColorAttachmentWrite(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT),
	DepthStencilAttachmentRead(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT),
	DepthStencilAttachmentWrite(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT),
	TransferRead(VK_ACCESS_TRANSFER_READ_BIT),
	TransferWrite(VK_ACCESS_TRANSFER_WRITE_BIT),
	HostRead(VK_ACCESS_HOST_READ_BIT),
	HostWrite(VK_ACCESS_HOST_WRITE_BIT),
	MemoryRead(VK_ACCESS_MEMORY_READ_BIT),
	MemoryWrite(VK_ACCESS_MEMORY_WRITE_BIT)
}
