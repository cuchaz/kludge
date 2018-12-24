/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.*
import java.util.*


inline fun <R> memstack(block: (MemoryStack) -> R): R {
	MemoryStack.stackPush().use { mem ->
		return block(mem)
	}
}

fun PointerBuffer.toStrings() = (0 until capacity()).map { getStringASCII() }

fun Collection<String>.toPointerBuffer(mem: MemoryStack) =
	mem.mallocPointer(size).apply {
		for (str in this@toPointerBuffer) {
			put(MemoryUtil.memASCII(str))
		}
		flip()
	}

fun IntBuffer.toList(size: Int) = (0 until size).map { get(it) }
fun LongBuffer.toList(size: Int) = (0 until size).map { get(it) }
fun FloatBuffer.toList(size: Int) = (0 until size).map { get(it) }
fun DoubleBuffer.toList(size: Int) = (0 until size).map { get(it) }

fun List<Int>.toBuffer(mem: MemoryStack) =
	mem.mallocInt(size).apply {
		for (i in this@toBuffer) {
			put(i)
		}
		flip()
	}

fun List<Long>.toBuffer(mem: MemoryStack) =
	mem.mallocLong(size).apply {
		for (i in this@toBuffer) {
			put(i)
		}
		flip()
	}

fun List<Float>.toBuffer(mem: MemoryStack) =
	mem.mallocFloat(size).apply {
		for (i in this@toBuffer) {
			put(i)
		}
		flip()
	}

fun List<Double>.toBuffer(mem: MemoryStack) =
	mem.mallocDouble(size).apply {
		for (i in this@toBuffer) {
			put(i)
		}
		flip()
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

// NOTE: making this function a member of IntFlags causes a compiler crash
// but an extension function is ok
fun <T:IntFlags.Bit> IntFlags<T>.toString(bits: Array<T>): String =
	bits
		.filter { has(it) }
		.joinToString()
