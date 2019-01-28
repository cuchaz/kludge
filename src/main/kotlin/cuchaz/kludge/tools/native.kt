/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import java.io.File
import java.nio.*
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


inline fun <R> memstack(block: (MemoryStack) -> R): R {
	MemoryStack.stackPush().use { mem ->
		return block(mem)
	}
}

fun String.toASCII(mem: MemoryStack): ByteBuffer = mem.ASCII(this)

fun PointerBuffer.toStrings() = (0 until capacity()).map { getStringASCII() }

fun Collection<String>.toStringPointerBuffer(mem: MemoryStack): PointerBuffer? =
	if (isEmpty()) {
		null
	} else {
		mem.mallocPointer(size).apply {
			for (str in this@toStringPointerBuffer) {
				put(str.toASCII(mem))
			}
			flip()
		}
	}

fun IntBuffer.toList(size: Int) = (0 until size).map { get(it) }
fun LongBuffer.toList(size: Int) = (0 until size).map { get(it) }
fun FloatBuffer.toList(size: Int) = (0 until size).map { get(it) }
fun DoubleBuffer.toList(size: Int) = (0 until size).map { get(it) }

fun Int.toBuffer(mem: MemoryStack): IntBuffer =
	mem.mallocInt(1).apply {
		put(this@toBuffer)
		flip()
	}
fun Collection<Int>.toBuffer(mem: MemoryStack): IntBuffer? =
	if (isEmpty()) {
		null
	} else {
		mem.mallocInt(size).apply {
			for (i in this@toBuffer) {
				put(i)
			}
			flip()
		}
	}

fun Long.toBuffer(mem: MemoryStack): LongBuffer =
	mem.mallocLong(1).apply {
		put(this@toBuffer)
		flip()
	}
fun Long.toPointerBuffer(mem: MemoryStack): PointerBuffer =
	mem.mallocPointer(1).apply {
		put(this@toPointerBuffer)
		flip()
	}
fun Collection<Long>.toBuffer(mem: MemoryStack): LongBuffer? =
	if (isEmpty()) {
		null
	} else {
		mem.mallocLong(size).apply {
			for (i in this@toBuffer) {
				put(i)
			}
			flip()
		}
	}
fun Collection<Long>.toPointerBuffer(mem: MemoryStack): PointerBuffer? =
	if (isEmpty()) {
		null
	} else {
		mem.mallocPointer(size).apply {
			for (i in this@toPointerBuffer) {
				put(i)
			}
			flip()
		}
	}

fun Float.toBuffer(mem: MemoryStack): FloatBuffer =
	mem.mallocFloat(1).apply {
		put(this@toBuffer)
		flip()
	}
fun Collection<Float>.toBuffer(mem: MemoryStack): FloatBuffer? =
	if (isEmpty()) {
		null
	} else {
		mem.mallocFloat(size).apply {
			for (i in this@toBuffer) {
				put(i)
			}
			flip()
		}
	}

fun Double.toBuffer(mem: MemoryStack): DoubleBuffer =
	mem.mallocDouble(1).apply {
		put(this@toBuffer)
		flip()
	}
fun Collection<Double>.toBuffer(mem: MemoryStack): DoubleBuffer? =
	if (isEmpty()) {
		null
	} else {
		mem.mallocDouble(size).apply {
			for (i in this@toBuffer) {
				put(i)
			}
			flip()
		}
	}


fun ByteBuffer.putInts(vararg values: Int) {
	for (v in values) {
		putInt(v)
	}
}
fun ByteBuffer.putLongs(vararg values: Long) {
	for (v in values) {
		putLong(v)
	}
}
fun ByteBuffer.putFloats(vararg values: Float) {
	for (v in values) {
		putFloat(v)
	}
}
fun ByteBuffer.putDoubles(vararg values: Double) {
	for (v in values) {
		putDouble(v)
	}
}

fun ByteBuffer.put(b: UByte) {
	put(b.toByte())
}


/** builds a UUID from the first 16 bytes in the buffer */
fun ByteBuffer.toUUID(): UUID {

	if (capacity() < 16) {
		throw IllegalArgumentException("byte buffer must be at least 16 bytes")
	}

	fun next() = get().toLong() and 0xff

	rewind()
	return UUID(
		(next() shl 8*7)
			or (next() shl 8*6)
			or (next() shl 8*5)
			or (next() shl 8*4)
			or (next() shl 8*3)
			or (next() shl 8*2)
			or (next() shl 8*1)
			or (next()),
		(next() shl 8*7)
			or (next() shl 8*6)
			or (next() shl 8*5)
			or (next() shl 8*4)
			or (next() shl 8*3)
			or (next() shl 8*2)
			or (next() shl 8*1)
			or (next())
	)
}


inline class IntFlags<T:IntFlags.Bit>(val value: Int) {

	companion object {

		fun <T:Bit> of(vararg bits: T): IntFlags<T> {
			var flags = IntFlags<T>(0)
			for (bit in bits) {
				flags = flags.set(bit)
			}
			return flags
		}

		fun <T:Bit> of(bits: Iterable<T>): IntFlags<T> {
			var flags = IntFlags<T>(0)
			for (bit in bits) {
				flags = flags.set(bit)
			}
			return flags
		}
	}

	interface Bit {
		val value: Int
	}

	fun has(bit: Bit) = (value and bit.value) != 0
	fun hasAny(other: IntFlags<T>) = (value and other.value) != 0
	fun hasAll(other: IntFlags<T>) = (value and other.value) == other.value

	fun set(bit: Bit) = IntFlags<T>(value or bit.value)
	fun setAll(other: IntFlags<T>) = IntFlags<T>(value or other.value)

	fun unset(bit: Bit) = IntFlags<T>(value and bit.value.inv())
	fun unsetAll(other: IntFlags<T>) = IntFlags<T>(value and other.value.inv())

	fun set(bit: Bit, value: Boolean) = if (value) set(bit) else unset(bit)
}

// NOTE: need to inline this with reified T to get the enum constants
// so, can't override the actual IntFlags.toString()
inline fun <reified T:IntFlags.Bit> IntFlags<T>.toFlagsString(): String =
	T::class.java.enumConstants
		.filter { has(it) }
		.joinToString(",")
		.let { "[$it]" }

fun Path.toByteBuffer(): ByteBuffer =
	FileChannel.open(this).map(FileChannel.MapMode.READ_ONLY, 0, Files.size(this))

fun File.toByteBuffer(): ByteBuffer = toPath().toByteBuffer()

/* TODO: do we need something like this?
fun InputStream.toByteBuffer(size: Int): ByteBuffer {
	val out = ByteBuffer.allocateDirect(size)
	var remaining = size
	while (remaining > 0) {
		out.put(buf)
		val read = read(result, offset, remaining)
		if (read < 0) break
		remaining -= read
		offset += read
	}
}
*/