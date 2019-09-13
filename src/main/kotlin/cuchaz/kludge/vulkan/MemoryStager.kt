/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.AutoCloser
import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.atLeast
import java.nio.ByteBuffer


/**
 * Manages transfer buffers for copying data between host and device.
 *
 * A single staging buffer is maintained for this device.
 * Each time a transfer is requested, the same staging buffer is used, so MemoryStager is not thead-safe.
 * If the existing staging buffer is too small for the request,
 * then that buffer is replaced with a new one that's big enough for the transfer.
 */
class MemoryStager internal constructor(
	val device: Device
) : AutoCloseable {

	private val closer = AutoCloser()
	private fun <R:AutoCloseable> R.autoClose(replace: R? = null) = apply { closer.add(this, replace) }
	override fun close() {
		closer.close()
	}

	// get the queue for memory transfers
	val queueFamily = device.physicalDevice.findQueueFamily(IntFlags.of(
		PhysicalDevice.QueueFamily.Flags.Transfer
	))
	val queues = device.queues[queueFamily]
		?: throw NoSuchElementException("no memory transfer queues defined for device, can't use MemoryStager")
	val queue = queues.firstOrNull() ?: throw NoSuchElementException("no memory transfer queue defined for device, can't use MemoryStager")

	// make a command buffer
	val commandPool = device
		.commandPool(
			queueFamily,
			flags = IntFlags.of(CommandPool.Create.ResetCommandBuffer)
		)
		.autoClose()

	// start with initial buffers
	private var buf: Buffer.Allocated? = null

	fun getBuffer(size: Long): Buffer.Allocated {

		// if the existing buffer is big enough, use that
		val existingBuf = buf
		if (existingBuf != null && existingBuf.memory.size >= size) {
			return existingBuf
		}

		// otherwise, allocate a new one (at least 1 KiB)
		var newSize = buf?.memory?.size?.atLeast(1024) ?: 1024
		while (newSize < size) {
			newSize *= 2
		}

		// allocate the new buffer
		val buf = device
			.buffer(
				size,
				IntFlags.of(Buffer.Usage.TransferSrc)
			)
			.autoClose(replace = buf?.buffer)
			.allocate { memType ->
				memType.flags.hasAll(IntFlags.of(
					MemoryType.Flags.HostVisible,
					MemoryType.Flags.HostCoherent
				))
			}
			.autoClose(replace = buf)

		this.buf = buf

		return buf
	}

	fun command(block: CommandBuffer.() -> Unit) {
		queue.submit(commandPool.buffer().apply {
			begin(IntFlags.of(CommandBuffer.Usage.OneTimeSubmit))
			block()
			end()
		})
		queue.waitForIdle()
	}
}


fun Buffer.allocateHost(): Buffer.Allocated =
	allocate { memType ->
		memType.flags.hasAll(IntFlags.of(
			MemoryType.Flags.HostVisible,
			MemoryType.Flags.HostCoherent
		))
	}

fun Buffer.allocateDevice(): Buffer.Allocated =
	allocate { memType ->
		memType.flags.hasAll(IntFlags.of(
			MemoryType.Flags.DeviceLocal
		))
	}

inline fun <T> Buffer.Allocated.transferHtoD(
	offset: Long = 0L,
	size: Int = memory.sizeAsInt,
	block: (buf: ByteBuffer) -> T
): T {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		return memory.map(offset, size, block)

	} else {

		// memory is not host-visible, so upload using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(size.toLong())
		val deviceBuf = this
		val result = hostBuf.memory.map(0, size, block)
		memory.device.memoryStager.command {
			copyBuffer(hostBuf.buffer, deviceBuf.buffer, 0, offset, size.toLong())
		}
		return result
	}
}

inline fun <T> Buffer.Allocated.transferDtoH(
	offset: Long = 0L,
	size: Int = memory.sizeAsInt,
	block: (buf: ByteBuffer) -> T
): T {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		return memory.map(offset, size, block)

	} else {

		// memory is not host-visible, so download using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(size.toLong())
		val deviceBuf = this
		memory.device.memoryStager.command {
			copyBuffer(deviceBuf.buffer, hostBuf.buffer, offset, 0, size.toLong())
		}
		return hostBuf.memory.map(0, size, block)
	}
}


fun Image.allocateDevice(): Image.Allocated =
	allocate { memType ->
		memType.flags.hasAll(IntFlags.of(
			MemoryType.Flags.DeviceLocal
		))
	}

inline fun <T> Image.Allocated.transferHtoD(
	layout: Image.Layout = Image.Layout.TransferDstOptimal,
	block: (buf: ByteBuffer) -> T
): T {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		return memory.map(0, memory.size.toInt(), block)

	} else {

		// memory is not host-visible, so upload using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(memory.size)
		val deviceImg = this
		val result = hostBuf.memory.map(0, memory.size.toInt(), block)
		memory.device.memoryStager.command {

			// transition image into a transfer destination
			pipelineBarrier(
				srcStage = IntFlags.of(PipelineStage.TopOfPipe),
				dstStage = IntFlags.of(PipelineStage.Transfer),
				images = listOf(
					image.barrier(
						dstAccess = IntFlags.of(Access.TransferWrite),
						newLayout = Image.Layout.TransferDstOptimal
					)
				)
			)

			// TODO: expose other image copy options? (need to emulate for host-visible mem tho)
			copyBufferToImage(hostBuf.buffer, deviceImg.image, layout)
		}
		return result
	}
}

inline fun <T> Image.Allocated.transferDtoH(
	layout: Image.Layout,
	block: (buf: ByteBuffer) -> T
): T {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		return memory.map(0, memory.size.toInt(), block)

	} else {

		// memory is not host-visible, so upload using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(memory.size)
		val deviceImg = this
		memory.device.memoryStager.command {

			// transition image into a transfer src
			pipelineBarrier(
				srcStage = IntFlags.of(PipelineStage.TopOfPipe),
				dstStage = IntFlags.of(PipelineStage.Transfer),
				images = listOf(
					image.barrier(
						dstAccess = IntFlags.of(Access.TransferRead),
						newLayout = Image.Layout.TransferSrcOptimal
					)
				)
			)

			// TODO: expose other image copy options? (need to emulate for host-visible mem tho)
			copyImageToBuffer(deviceImg.image, hostBuf.buffer, layout)
		}
		return hostBuf.memory.map(0, memory.size.toInt(), block)
	}
}
