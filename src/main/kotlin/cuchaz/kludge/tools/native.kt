/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryUtil


fun PointerBuffer.toStrings() = (0 until capacity()).map { getStringASCII() }

fun Collection<String>.toPointerBuffer() =
	MemoryUtil.memAllocPointer(size).apply {
		for (str in this@toPointerBuffer) {
			put(MemoryUtil.memASCII(str))
		}
		flip()
	}


inline class IntFlags(val value: Int) {

	companion object {

		fun <T:Bit> of(vararg bits: T): IntFlags {
			var flags = IntFlags(0)
			for (bit in bits) {
				flags = flags.set(bit)
			}
			return flags
		}

		fun <T:Bit> of(bits: Iterable<T>): IntFlags {
			var flags = IntFlags(0)
			for (bit in bits) {
				flags = flags.set(bit)
			}
			return flags
		}
	}

	interface Bit {
		val value: Int
	}

	operator fun get(bit: Bit) = value and bit.value != 0

	fun set(bit: Bit) = IntFlags(value or bit.value)
	fun unset(bit: Bit) = IntFlags(value and bit.value.inv())

	fun set(bit: Bit, value: Boolean) = if (value) set(bit) else unset(bit)
}
