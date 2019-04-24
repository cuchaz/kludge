package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.AutoCloser
import cuchaz.kludge.tools.IntFlags
import java.nio.ByteBuffer


/**
 * Manages transfer buffers for copying data between host and device.
 *
 * A single staging buffer is maintained for this device.
 * Each time a transfer is requested, the same staging buffer is used, so MemoryStager is not thead-safe.
 * If the existing staging buffer is too small for the request,
 * then that buffer is replaced with a new one that's big enough for the transfer.
 *
 * TODO: find a computer with a discrete GPU to test this!
 */
class MemoryStager internal constructor(
	val device: Device
) : AutoCloseable {

	private val closer = AutoCloser()
	private fun <R:AutoCloseable> R.autoClose() = also { closer.add(this@autoClose) }
	override fun close() {
		closer.close()

		// also cleanup the staging buffers
		buf.memory.close()
		buf.close()
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
	private var buf: Buffer.Allocated = allocate(1024)

	private fun allocate(size: Long) = device
		.buffer(
			size,
			IntFlags.of(Buffer.Usage.TransferSrc)
		)
		.autoClose()
		.allocate { memType ->
			memType.flags.hasAll(IntFlags.of(
				MemoryType.Flags.HostVisible,
				MemoryType.Flags.HostCoherent
			))
		}
		.autoClose()

	fun getBuffer(size: Long): Buffer.Allocated {

		// if the existing buffer is big enough, use that
		if (buf.memory.size >= size) {
			return buf
		}

		// otherwise, allocate a new one
		var newSize = buf.memory.size
		while (newSize < size) {
			newSize *= 2
		}

		// cleanup the old buffer
		buf.memory.close()
		buf.close()

		// allocate the new buffer
		buf = allocate(newSize)

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


fun Buffer.allocateDevice(): Buffer.Allocated =
	allocate { memType ->
		memType.flags.hasAll(IntFlags.of(
			MemoryType.Flags.DeviceLocal
		))
	}

inline fun <T> Buffer.Allocated.transferHtoD(offset: Long = 0L, size: Int = memory.sizeAsInt, block: (buf: ByteBuffer) -> T) {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		memory.map(offset, size, block)

	} else {

		// memory is not host-visible, so upload using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(size.toLong())
		val deviceBuf = this
		hostBuf.memory.map(0, size, block)
		memory.device.memoryStager.command {
			copyBuffer(hostBuf.buffer, deviceBuf.buffer, 0, offset, size.toLong())
		}
	}
}

inline fun <T> Buffer.Allocated.transferDtoH(offset: Long = 0L, size: Int = memory.sizeAsInt, block: (buf: ByteBuffer) -> T) {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		memory.map(offset, size, block)

	} else {

		// memory is not host-visible, so download using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(size.toLong())
		val deviceBuf = this
		memory.device.memoryStager.command {
			copyBuffer(deviceBuf.buffer, hostBuf.buffer, offset, 0, size.toLong())
		}
		hostBuf.memory.map(0, size, block)
	}
}


fun Image.allocateDevice(): Image.Allocated =
	allocate { memType ->
		memType.flags.hasAll(IntFlags.of(
			MemoryType.Flags.DeviceLocal
		))
	}

inline fun <T> Image.Allocated.transferHtoD(
	layout: Image.Layout,
	rowLength: Int = 0,
	height: Int = 0,
	range: Image.SubresourceLayers = Image.SubresourceLayers(),
	offset: Offset3D = Offset3D(0, 0, 0),
	extent: Extent3D = this.image.extent,
	block: (buf: ByteBuffer) -> T
) {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		memory.map(0, memory.size.toInt(), block)

	} else {

		// memory is not host-visible, so upload using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(memory.size)
		val deviceImg = this
		hostBuf.memory.map(0, memory.size.toInt(), block)
		memory.device.memoryStager.command {
			copyBufferToImage(hostBuf.buffer, deviceImg.image, layout, 0, rowLength, height, range, offset, extent)
		}
	}
}

inline fun <T> Image.Allocated.transferDtoH(
	layout: Image.Layout,
	rowLength: Int = 0,
	height: Int = 0,
	range: Image.SubresourceLayers = Image.SubresourceLayers(),
	offset: Offset3D = Offset3D(0, 0, 0),
	extent: Extent3D = this.image.extent,
	block: (buf: ByteBuffer) -> T
) {
	if (memory.type.flags.has(MemoryType.Flags.HostVisible)) {

		// memory is host-visible, so map it directly
		memory.map(0, memory.size.toInt(), block)

	} else {

		// memory is not host-visible, so upload using a staging buffer
		val hostBuf = memory.device.memoryStager.getBuffer(memory.size)
		val deviceImg = this
		memory.device.memoryStager.command {
			copyImageToBuffer(deviceImg.image, hostBuf.buffer, layout, 0, rowLength, height, range, offset, extent)
		}
		hostBuf.memory.map(0, memory.size.toInt(), block)
	}
}
