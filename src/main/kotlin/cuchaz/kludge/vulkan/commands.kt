/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class CommandPool(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	enum class CreateFlags(override val value: Int) : IntFlags.Bit {
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
}

fun Device.commandPool(
	queueFamily: PhysicalDevice.QueueFamily,
	flags: IntFlags<CommandPool.CreateFlags> = IntFlags(0)
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

	enum class UsageFlags(override val value: Int) : IntFlags.Bit {
		OneTimeSubmit(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT),
		RenderPassContinue(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT),
		SimultaneousUse(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
	}

	fun begin(
		flags: IntFlags<UsageFlags> = IntFlags(0)
	) {
		memstack { mem ->
			val info = VkCommandBufferBeginInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
				.flags(flags.value)
				.pInheritanceInfo(null) // TODO: support secondary queues?
			vkBeginCommandBuffer(vkBuf, info)
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
		graphicsPipeline: GraphicsPipeline,
		framebuffer: Framebuffer,
		renderArea: Rect2D,
		clearValue: ClearValue,
		contents: Contents = Contents.Inline
	) {
		memstack { mem ->
			val info = VkRenderPassBeginInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
				.renderPass(graphicsPipeline.renderPassId)
				.framebuffer(framebuffer.id)
				.renderArea(renderArea.toVulkan(mem))
				.pClearValues(clearValue.toBuffer(mem))
			vkCmdBeginRenderPass(vkBuf, info, contents.ordinal)
		}
	}

	fun endRenderPass() {
		vkCmdEndRenderPass(vkBuf)
	}

	fun bind(graphicsPipeline: GraphicsPipeline) {
		vkCmdBindPipeline(vkBuf, Subpass.PipelineBindPoint.Graphics.ordinal, graphicsPipeline.id)
	}

	fun draw(
		vertices: Int,
		instances: Int = 1,
		firstVertex: Int = 0,
		firstInstance: Int = 0
	) {
		vkCmdDraw(vkBuf, vertices, instances, firstVertex, firstInstance)
	}
}


sealed class ClearValue {

	sealed class Color : ClearValue() {

		data class Float(
			val r: kotlin.Float = 0.0f,
			val g: kotlin.Float = 0.0f,
			val b: kotlin.Float = 0.0f,
			val a: kotlin.Float = 1.0f
		) : ClearValue.Color() {
			internal fun toVulkan(mem: MemoryStack) = VkClearValue.mallocStack(mem).set(this)
		}

		data class Int(
			val r: kotlin.Int = 0,
			val g: kotlin.Int = 0,
			val b: kotlin.Int = 0,
			val a: kotlin.Int = 0
		) : ClearValue.Color() {
			internal fun toVulkan(mem: MemoryStack) = VkClearValue.mallocStack(mem).set(this)
		}
	}

	class DepthStencil(
		val depth: Float = 0.0f,
		val stencil: Int = 0
	) : ClearValue() {
		internal fun toVulkan(mem: MemoryStack) = VkClearValue.mallocStack(mem).set(this)
	}

	internal fun toBuffer(mem: MemoryStack) =
		VkClearValue.mallocStack(1, mem)
			.apply {
				get().set(this@ClearValue)
				flip()
			}
}
internal fun VkClearValue.set(value: ClearValue) =
	apply {
		when (value) {
			is ClearValue.Color.Float -> color().apply {
				float32(0, value.r)
				float32(1, value.g)
				float32(2, value.b)
				float32(3, value.a)
			}
			is ClearValue.Color.Int -> color().apply {
				int32(0, value.r)
				int32(1, value.g)
				int32(2, value.b)
				int32(3, value.a)
			}
			is ClearValue.DepthStencil -> depthStencil().apply {
				depth(value.depth)
				stencil(value.stencil)
			}
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
