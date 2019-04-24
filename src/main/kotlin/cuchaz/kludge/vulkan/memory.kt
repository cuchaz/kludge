/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import cuchaz.kludge.tools.toIntOrNull
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import java.nio.ByteBuffer


class Buffer internal constructor(
	val device: Device,
	internal val id: Long,
	val size: Long
) : AutoCloseable {

	enum class Flags(override val value: Int) : IntFlags.Bit {
		SparseBinding(VK_BUFFER_CREATE_SPARSE_BINDING_BIT),
		SparseResidency(VK_BUFFER_CREATE_SPARSE_RESIDENCY_BIT),
		SparseAliased(VK_BUFFER_CREATE_SPARSE_ALIASED_BIT)
	}

	enum class Usage(override val value: Int) : IntFlags.Bit {
		TransferSrc(VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
		TransferDst(VK_BUFFER_USAGE_TRANSFER_DST_BIT),
		UniformTexelBuffer(VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT),
		StorageTexelBuffer(VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT),
		UniformBuffer(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
		StorageBuffer(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT),
		IndexBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
		VertexBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
		IndirectBuffer(VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT)
	}

	override fun close() {
		vkDestroyBuffer(device.vkDevice, id, null)
	}

	val memoryRequirements: MemoryRequirements by lazy {
		memstack { mem ->
			val pMem = VkMemoryRequirements.mallocStack(mem)
			vkGetBufferMemoryRequirements(device.vkDevice, id, pMem)
			MemoryRequirements(
				device.physicalDevice,
				pMem.size(),
				pMem.alignment(),
				pMem.memoryTypeBits()
			)
		}
	}

	fun bindTo(mem: MemoryAllocation, offset: Long = 0L) =
		device.bindBufferMemory(this, mem, offset)

	inner class Allocated(
		val memory: MemoryAllocation
	) : AutoCloseable {

		val buffer: Buffer = this@Buffer

		override fun close() {
			memory.close()
		}
	}

	fun allocate(memType: MemoryType): Allocated {
		val mem = device.allocateMemory(memoryRequirements.size, memType)
		bindTo(mem)
		return Allocated(mem)
	}

	fun allocate(memTypeFilter: (MemoryType) -> Boolean) =
		allocate(memoryRequirements.memoryTypes
			.firstOrNull(memTypeFilter)
			?: throw NoSuchElementException("no suitable memory type")
		)
}

fun Device.buffer(
	size: Long,
	usage: IntFlags<Buffer.Usage>,
	concurrentQueueFamilies: Set<PhysicalDevice.QueueFamily> = emptySet(),
	flags: IntFlags<Buffer.Flags> = IntFlags(0)
): Buffer {
	memstack { mem ->

		val info = VkBufferCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
			.size(size)
			.flags(flags.value)
			.usage(usage.value)
		if (concurrentQueueFamilies.isEmpty()) {
			info.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
		} else {
			info.sharingMode(VK_SHARING_MODE_CONCURRENT)
			info.pQueueFamilyIndices(concurrentQueueFamilies.map { it.index }.toBuffer(mem))
		}

		val pBuf = mem.mallocLong(1)
		vkCreateBuffer(vkDevice, info, null, pBuf)
			.orFail("failed to create buffer")
		return Buffer(this, pBuf.get(0), size)
	}
}


class MemoryRequirements(
	val physicalDevice: PhysicalDevice,
	val size: Long,
	val alignment: Long,
	internal val memoryTypeBits: Int
) {

	val memoryTypes: List<MemoryType> by lazy {
		physicalDevice.memoryTypes
			.filter { (1 shl it.index) and memoryTypeBits != 0 }
	}
}


class MemoryAllocation(
	val device: Device,
	internal val id: Long,
	val size: Long,
	val type: MemoryType
) : AutoCloseable {

	override fun close() {
		vkFreeMemory(device.vkDevice, id, null)
	}

	val sizeAsInt: Int get() =
		size
			.toIntOrNull()
			?: throw IllegalStateException(
				"buffer size ($size bytes) too large to map all at once."
				+ " Try mapping buffer slices up to ${Int.MAX_VALUE} bytes instead."
			)

	fun map(offset: Long = 0L, size: Int = sizeAsInt): ByteBuffer {
		memstack { mem ->
			val ppData = mem.mallocPointer(1)
			val flags = 0 // reserved for future use
			vkMapMemory(device.vkDevice, id, offset, size.toLong(), flags, ppData)
				.orFail("failed to map device memory")
			return MemoryUtil.memByteBuffer(ppData.get(0), size)
		}
	}

	fun unmap() {
		vkUnmapMemory(device.vkDevice, id)
	}

	inline fun <T> map(offset: Long = 0L, size: Int = sizeAsInt, block: (buf: ByteBuffer) -> T) =
		try {
			block(map(offset, size))
		} finally {
			unmap()
		}

	// TODO: support flush/invalidate for mapped memory ranges?
}

fun Device.allocateMemory(size: Long, memType: MemoryType): MemoryAllocation {
	memstack { mem ->
		val info = VkMemoryAllocateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
			.allocationSize(size)
			.memoryTypeIndex(memType.index)
		val pMem = mem.mallocLong(1)
		vkAllocateMemory(vkDevice, info, null, pMem)
			.orFail("failed to allocate memory")
		/* TODO: max number of allocations might be very small (4096)
			see PhysicalDevice.properties.limits.maxMemoryAllocationCount
			https://gamedev.stackexchange.com/questions/163933/why-do-gpus-have-limited-amount-of-allocations
		 */
		return MemoryAllocation(this, pMem.get(0), size, memType)
	}
}

fun Device.bindBufferMemory(buf: Buffer, mem: MemoryAllocation, offset: Long = 0L) {
	vkBindBufferMemory(vkDevice, buf.id, mem.id, offset)
		.orFail("failed to bind buffer to device memory")
}
