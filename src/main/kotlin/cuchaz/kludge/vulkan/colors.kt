/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.put
import java.nio.ByteBuffer


sealed class ColorRGBA {

	data class Float(
		val r: kotlin.Float = 0.0f,
		val g: kotlin.Float = 0.0f,
		val b: kotlin.Float = 0.0f,
		val a: kotlin.Float = 1.0f
	) : ColorRGBA() {

		override val ri = (r*255).toInt()
		override val gi = (g*255).toInt()
		override val bi = (b*255).toInt()
		override val ai = (a*255).toInt()

		override val rf = r
		override val gf = g
		override val bf = b
		override val af = a

		override fun toString() = "($r,$g,$b,$a)"
	}

	data class Int(
		val r: kotlin.Int = 0,
		val g: kotlin.Int = 0,
		val b: kotlin.Int = 0,
		val a: kotlin.Int = 255
	) : ColorRGBA() {

		constructor(hex: UInt) : this(
			((hex and 0xff000000u) shr 8*3).toInt(),
			((hex and 0x00ff0000u) shr 8*2).toInt(),
			((hex and 0x0000ff00u) shr 8).toInt(),
			(hex and 0x000000ffu).toInt()
		)

		override val ri = r
		override val gi = g
		override val bi = b
		override val ai = a

		override val rf = r.toFloat()/255
		override val gf = g.toFloat()/255
		override val bf = b.toFloat()/255
		override val af = a.toFloat()/255

		override fun toString() = "($r,$g,$b,$a)"
	}

	abstract val ri: kotlin.Int
	abstract val gi: kotlin.Int
	abstract val bi: kotlin.Int
	abstract val ai: kotlin.Int

	abstract val rf: kotlin.Float
	abstract val gf: kotlin.Float
	abstract val bf: kotlin.Float
	abstract val af: kotlin.Float

	fun toClearColor(): ClearValue.Color = ClearValue.Color.Float(rf, gf, bf, af)
}


fun ByteBuffer.putColor4Bytes(color: ColorRGBA) {
	put(color.ri.toUByte())
	put(color.gi.toUByte())
	put(color.bi.toUByte())
	put(color.ai.toUByte())
}

fun ByteBuffer.putColor4Floats(color: ColorRGBA) {
	putFloat(color.rf)
	putFloat(color.gf)
	putFloat(color.bf)
	putFloat(color.af)
}
