/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer


class CommandPool(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	enum class Create(override val value: Int) : IntFlags.Bit {
		Transient(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT),
		ResetCommandBuffer(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
	}

	override fun close() {
		vkDestroyCommandPool(device.vkDevice, id, null)
	}

	enum class Level {
		Primary,
		Secondary
	}

	fun buffer(
		level: Level = Level.Primary
	): CommandBuffer {
		memstack { mem ->

			val info = VkCommandBufferAllocateInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
				.commandPool(id)
				.level(level.ordinal)
				.commandBufferCount(1)

			val pBuffers = mem.mallocPointer(1)
			vkAllocateCommandBuffers(device.vkDevice, info, pBuffers)
				.orFail("failed to allocate command buffer")
			return CommandBuffer(this, pBuffers.get(0))
		}
	}

	enum class Reset(override val value: Int) : IntFlags.Bit {
		ReleaseResources(VK_COMMAND_POOL_RESET_RELEASE_RESOURCES_BIT)
	}

	fun reset(flags: IntFlags<Reset> = IntFlags(0)) {
		vkResetCommandPool(device.vkDevice, id, flags.value)
			.orFail("failed to reset command pool")
	}
}

fun Device.commandPool(
	queueFamily: PhysicalDevice.QueueFamily,
	flags: IntFlags<CommandPool.Create> = IntFlags(0)
): CommandPool {
	memstack { mem ->

		val info = VkCommandPoolCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
			.flags(flags.value)
			.queueFamilyIndex(queueFamily.index)

		val pCommandPool = mem.mallocLong(1)
		vkCreateCommandPool(vkDevice, info, null, pCommandPool)
			.orFail("failed to create command pool")
		return CommandPool(this, pCommandPool.get(0))
	}
}


class CommandBuffer internal constructor(
	val pool: CommandPool,
	internal val id: Long
) {
	internal val vkBuf = VkCommandBuffer(id, pool.device.vkDevice)

	enum class Usage(override val value: Int) : IntFlags.Bit {
		OneTimeSubmit(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT),
		RenderPassContinue(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT),
		SimultaneousUse(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
	}

	enum class Reset(override val value: Int) : IntFlags.Bit {
		ReleaseResources(VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)
	}

	fun reset(flags: IntFlags<Reset> = IntFlags(0)) {
		vkResetCommandBuffer(vkBuf, flags.value)
			.orFail("failed to reset command buffer")
	}

	fun begin(flags: IntFlags<Usage> = IntFlags(0)) {
		memstack { mem ->
			val info = VkCommandBufferBeginInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
				.flags(flags.value)
				.pInheritanceInfo(null) // TODO: support secondary queues?
			vkBeginCommandBuffer(vkBuf, info)
				.orFail("failed to begin command buffer")
		}

	}

	fun end() {
		vkEndCommandBuffer(vkBuf)
			.orFail("failed to end command buffer")
	}

	enum class Contents {
		Inline,
		SecondaryCommandBuffers
	}

	fun beginRenderPass(
		renderPass: RenderPass,
		framebuffer: Framebuffer,
		renderArea: Rect2D,
		clearValues: Map<Attachment,ClearValue?>,
		contents: Contents = Contents.Inline
	) {
		memstack { mem ->
			val info = VkRenderPassBeginInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
				.renderPass(renderPass.id)
				.framebuffer(framebuffer.id)
				.renderArea { it.set(renderArea) }
				.pClearValues(VkClearValue.callocStack(renderPass.attachments.size, mem).apply {
					for (attachment in renderPass.attachments) {
						val out = get()
						clearValues[attachment]?.let { out.set(it) }
					}
					flip()
				})
			vkCmdBeginRenderPass(vkBuf, info, contents.ordinal)
		}
	}

	fun endRenderPass() {
		vkCmdEndRenderPass(vkBuf)
	}

	fun bindPipeline(pipeline: Pipeline) {
		vkCmdBindPipeline(vkBuf, pipeline.bindPoint.ordinal, pipeline.id)
	}

	fun bindVertexBuffer(buffer: Buffer, offset: Long = 0L) {
		memstack { mem ->
			vkCmdBindVertexBuffers(vkBuf, 0, buffer.id.toBuffer(mem), offset.toBuffer(mem))
		}
	}

	enum class IndexType {
		UInt16,
		UInt32
	}

	fun bindIndexBuffer(buffer: Buffer, indexType: IndexType, offset: Long = 0L) {
		memstack { mem ->
			vkCmdBindIndexBuffer(vkBuf, buffer.id, offset, indexType.ordinal)
		}
	}

	fun bindDescriptorSet(
		descriptorSet: DescriptorSet,
		pipeline: Pipeline
	) {
		memstack { mem ->
			val pOffsets = null // TODO: support offsets?
			val pSets = descriptorSet.id.toBuffer(mem)
			vkCmdBindDescriptorSets(vkBuf, pipeline.bindPoint.ordinal, pipeline.layoutId, 0, pSets, pOffsets)
		}
	}

	fun bindDescriptorSets(
		descriptorSets: List<DescriptorSet>,
		pipeline: Pipeline
	) {
		memstack { mem ->
			val pOffsets = null // TODO: support offsets?
			val pSets = descriptorSets.map { it.id }.toBuffer(mem) ?: throw IllegalArgumentException("no descriptor sets given")
			vkCmdBindDescriptorSets(vkBuf, pipeline.bindPoint.ordinal, pipeline.layoutId, 0, pSets, pOffsets)
		}
	}

	fun pushConstants(
		pipeline: Pipeline,
		stageFlags: IntFlags<ShaderStage>,
		buf: ByteBuffer,
		offset: Int = 0
	) {
		vkCmdPushConstants(vkBuf, pipeline.layoutId, stageFlags.value, offset, buf)
	}

	fun pushConstants(
		pipeline: Pipeline,
		stageFlags: IntFlags<ShaderStage>,
		vararg values: Short,
		offset: Int = 0
	) {
		vkCmdPushConstants(vkBuf, pipeline.layoutId, stageFlags.value, offset, values)
	}

	fun pushConstants(
		pipeline: Pipeline,
		stageFlags: IntFlags<ShaderStage>,
		vararg values: Int,
		offset: Int = 0
	) {
		vkCmdPushConstants(vkBuf, pipeline.layoutId, stageFlags.value, offset, values)
	}

	fun pushConstants(
		pipeline: Pipeline,
		stageFlags: IntFlags<ShaderStage>,
		vararg values: Long,
		offset: Int = 0
	) {
		vkCmdPushConstants(vkBuf, pipeline.layoutId, stageFlags.value, offset, values)
	}

	fun pushConstants(
		pipeline: Pipeline,
		stageFlags: IntFlags<ShaderStage>,
		vararg values: Float,
		offset: Int = 0
	) {
		vkCmdPushConstants(vkBuf, pipeline.layoutId, stageFlags.value, offset, values)
	}

	fun pushConstants(
		pipeline: Pipeline,
		stageFlags: IntFlags<ShaderStage>,
		vararg values: Double,
		offset: Int = 0
	) {
		vkCmdPushConstants(vkBuf, pipeline.layoutId, stageFlags.value, offset, values)
	}

	fun draw(
		vertices: Int,
		instances: Int = 1,
		firstVertex: Int = 0,
		firstInstance: Int = 0
	) {
		vkCmdDraw(vkBuf, vertices, instances, firstVertex, firstInstance)
	}

	fun drawIndexed(
		indices: Int,
		instances: Int = 1,
		firstIndex: Int = 0,
		firstInstance: Int = 0,
		vertexOffset: Int = 0
	) {
		vkCmdDrawIndexed(vkBuf, indices, instances, firstIndex, vertexOffset, firstInstance)
	}

	fun dispatch(
		x: Int,
		y: Int = 1,
		z: Int = 1
	) {
		vkCmdDispatch(vkBuf, x, y, z)
	}

	fun dispatch(extent: Extent2D) = dispatch(extent.width, extent.height)
	fun dispatch(extent: Extent3D) = dispatch(extent.width, extent.height, extent.depth)

	fun copyBuffer(
		src: Buffer,
		dst: Buffer,
		srcOffset: Long = 0,
		dstOffset: Long = 0,
		size: Long = src.size
	) {
		memstack { mem ->
			val pRegions = VkBufferCopy.callocStack(1, mem)
			pRegions.get()
				.srcOffset(srcOffset)
				.dstOffset(dstOffset)
				.size(size)
			pRegions.flip()
			vkCmdCopyBuffer(vkBuf, src.id, dst.id, pRegions)
		}
	}

	fun copyBufferToImage(
		src: Buffer,
		dst: Image,
		dstLayout: Image.Layout,
		srcOffset: Long = 0L,
		rowLength: Int = 0,
		height: Int = 0,
		range: Image.SubresourceLayers = Image.SubresourceLayers(),
		offset: Offset3D = Offset3D(0, 0, 0),
		extent: Extent3D = dst.extent
	) {
		memstack { mem ->
			val pRegions = VkBufferImageCopy.callocStack(1, mem)
			pRegions.get()
				.bufferOffset(srcOffset)
				.bufferRowLength(rowLength)
				.bufferImageHeight(height)
				.imageSubresource { it.set(range) }
				.imageOffset { it.set(offset) }
				.imageExtent { it.set(extent) }
			pRegions.flip()
			vkCmdCopyBufferToImage(vkBuf, src.id, dst.id, dstLayout.value, pRegions)
		}
	}

	fun copyImageToBuffer(
		src: Image,
		dst: Buffer,
		srcLayout: Image.Layout,
		dstOffset: Long = 0L,
		rowLength: Int = 0,
		height: Int = 0,
		range: Image.SubresourceLayers = Image.SubresourceLayers(),
		offset: Offset3D = Offset3D(0, 0, 0),
		extent: Extent3D = src.extent
	) {
		memstack { mem ->
			val pRegions = VkBufferImageCopy.callocStack(1, mem)
			pRegions.get()
				.bufferOffset(dstOffset)
				.bufferRowLength(rowLength)
				.bufferImageHeight(height)
				.imageSubresource { it.set(range) }
				.imageOffset { it.set(offset) }
				.imageExtent { it.set(extent) }
			pRegions.flip()
			vkCmdCopyImageToBuffer(vkBuf, src.id, srcLayout.value, dst.id, pRegions)
		}
	}

	data class MemoryBarrier(
		val srcAccess: IntFlags<Access>,
		val dstAccess: IntFlags<Access>
	)

	data class BufferBarrier internal constructor(
		val buffer: Buffer,
		val srcAccess: IntFlags<Access>,
		val dstAccess: IntFlags<Access>,
		val offset: Long,
		val size: Long,
		val srcQueueFamily: PhysicalDevice.QueueFamily?,
		val dstQueueFamily: PhysicalDevice.QueueFamily?
	)

	data class ImageBarrier internal constructor(
		val image: Image,
		val srcAccess: IntFlags<Access>,
		val dstAccess: IntFlags<Access>,
		val oldLayout: Image.Layout,
		val newLayout: Image.Layout,
		val range: Image.SubresourceRange,
		val srcQueueFamily: PhysicalDevice.QueueFamily?,
		val dstQueueFamily: PhysicalDevice.QueueFamily?
	)

	fun pipelineBarrier(
		dstStage: IntFlags<PipelineStage>,
		srcStage: IntFlags<PipelineStage>,
		dependencyFlags: IntFlags<Dependency> = IntFlags(0),
		memories: List<MemoryBarrier> = emptyList(),
		buffers: List<BufferBarrier> = emptyList(),
		images: List<ImageBarrier> = emptyList()
	) {
		memstack { mem ->

			// memory barriers
			val pMemBarriers = VkMemoryBarrier.callocStack(images.size, mem)
			for (m in memories) {
				pMemBarriers.get()
					.sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER)
					.srcAccessMask(m.srcAccess.value)
					.dstAccessMask(m.dstAccess.value)
			}
			pMemBarriers.flip()

			// buffer memory barriers
			val pBufBarriers = VkBufferMemoryBarrier.callocStack(buffers.size, mem)
			for  (b in buffers) {
				pBufBarriers.get()
					.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
					.srcAccessMask(b.srcAccess.value)
					.dstAccessMask(b.dstAccess.value)
					.srcQueueFamilyIndex(b.srcQueueFamily?.index ?: VK_QUEUE_FAMILY_IGNORED)
					.dstQueueFamilyIndex(b.dstQueueFamily?.index ?: VK_QUEUE_FAMILY_IGNORED)
					.buffer(b.buffer.id)
					.offset(b.offset)
					.size(b.size)
			}
			pBufBarriers.flip()

			// image memory barriers
			val pImgBarriers = VkImageMemoryBarrier.callocStack(images.size, mem)
			for (i in images) {
				pImgBarriers.get()
					.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
					.srcAccessMask(i.srcAccess.value)
					.dstAccessMask(i.dstAccess.value)
					.oldLayout(i.oldLayout.value)
					.newLayout(i.newLayout.value)
					.srcQueueFamilyIndex(i.srcQueueFamily?.index ?: VK_QUEUE_FAMILY_IGNORED)
					.dstQueueFamilyIndex(i.dstQueueFamily?.index ?: VK_QUEUE_FAMILY_IGNORED)
					.image(i.image.id)
					.subresourceRange { it.set(i.range) }
			}
			pImgBarriers.flip()

			vkCmdPipelineBarrier(vkBuf, srcStage.value, dstStage.value, dependencyFlags.value, pMemBarriers, pBufBarriers, pImgBarriers)
		}
	}

	fun clearImage(
		image: Image,
		imageLayout: Image.Layout,
		clearColor: ClearValue.Color,
		range: Image.SubresourceRange = Image.SubresourceRange()
	) {
		memstack { mem ->
			val pClearColor = VkClearColorValue.callocStack(mem).apply { set(clearColor) }
			val pRange = VkImageSubresourceRange.callocStack(mem).apply { set(range) }
			vkCmdClearColorImage(vkBuf, image.id, imageLayout.value, pClearColor, pRange)
		}
	}

	fun clearImage(
		image: Image,
		imageLayout: Image.Layout,
		clearDepthStencil: ClearValue.DepthStencil,
		range: Image.SubresourceRange = Image.SubresourceRange()
	) {
		memstack { mem ->
			val pClearDepthStencil = VkClearDepthStencilValue.callocStack(mem).apply { set(clearDepthStencil) }
			val pRange = VkImageSubresourceRange.callocStack(mem).apply { set(range) }
			vkCmdClearDepthStencilImage(vkBuf, image.id, imageLayout.value, pClearDepthStencil, pRange)
		}
	}
}


sealed class ClearValue {

	sealed class Color : ClearValue() {

		data class Float(
			val r: kotlin.Float = 0.0f,
			val g: kotlin.Float = 0.0f,
			val b: kotlin.Float = 0.0f,
			val a: kotlin.Float = 1.0f
		) : ClearValue.Color()

		data class Int(
			val r: kotlin.Int = 0,
			val g: kotlin.Int = 0,
			val b: kotlin.Int = 0,
			val a: kotlin.Int = 0
		) : ClearValue.Color()
	}

	class DepthStencil(
		val depth: Float = 0.0f,
		val stencil: Int = 0
	) : ClearValue()
}

internal fun VkClearColorValue.set(value: ClearValue.Color) =
	apply {
		when (value) {
			is ClearValue.Color.Float -> {
				float32(0, value.r)
				float32(1, value.g)
				float32(2, value.b)
				float32(3, value.a)
			}
			is ClearValue.Color.Int -> {
				int32(0, value.r)
				int32(1, value.g)
				int32(2, value.b)
				int32(3, value.a)
			}
		}
	}
internal fun VkClearDepthStencilValue.set(value: ClearValue.DepthStencil) =
	apply {
		depth(value.depth)
		stencil(value.stencil)
	}
internal fun VkClearValue.set(value: ClearValue) =
	apply {
		when (value) {
			is ClearValue.Color -> color().set(value)
			is ClearValue.DepthStencil -> depthStencil().set(value)
		}
	}
internal fun VkClearValue.toClearValueColorFloat() =
	ClearValue.Color.Float(
		color().float32(0),
		color().float32(1),
		color().float32(2),
		color().float32(3)
	)
internal fun VkClearValue.toClearValueColorInt() =
	ClearValue.Color.Int(
		color().int32(0),
		color().int32(1),
		color().int32(2),
		color().int32(3)
	)
internal fun VkClearValue.toClearValueDepthStencil() =
	ClearValue.DepthStencil(
		depthStencil().depth(),
		depthStencil().stencil()
	)
internal fun Collection<ClearValue>.toBuffer(mem: MemoryStack) =
	if (isEmpty()) {
		null
	} else {
		VkClearValue.mallocStack(size, mem).apply {
			for (c in this@toBuffer) {
				get().set(c)
			}
			flip()
		}
	}


fun Image.barrier(
	dstAccess: IntFlags<Access>,
	newLayout: Image.Layout,
	srcAccess: IntFlags<Access> = IntFlags(0),
	oldLayout: Image.Layout = Image.Layout.Undefined,
	range: Image.SubresourceRange = Image.SubresourceRange(),
	srcQueueFamily: PhysicalDevice.QueueFamily? = null,
	dstQueueFamily: PhysicalDevice.QueueFamily? = null
) = CommandBuffer.ImageBarrier(this, srcAccess, dstAccess, oldLayout, newLayout, range, srcQueueFamily, dstQueueFamily)

fun Buffer.barrier(
	dstAccess: IntFlags<Access>,
	srcAccess: IntFlags<Access> = IntFlags(0),
	offset: Long = 0L,
	size: Long = this.size,
	srcQueueFamily: PhysicalDevice.QueueFamily? = null,
	dstQueueFamily: PhysicalDevice.QueueFamily? = null
) = CommandBuffer.BufferBarrier(this, srcAccess, dstAccess, offset, size, srcQueueFamily, dstQueueFamily)
