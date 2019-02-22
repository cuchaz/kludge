/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import java.lang.Math.max
import java.lang.Math.min


fun Int.atLeast(i: Int) = max(this, i)
fun Int.atMost(i: Int) = min(this, i)
fun Int.clamp(a: Int, b: Int) = this.atLeast(a).atMost(b)
fun Int.divideUp(i: Int) = (this + i - 1)/i

fun Long.atLeast(l: Long) = max(this, l)
fun Long.atMost(l: Long) = min(this, l)
fun Long.clamp(a: Long, b: Long) = this.atLeast(a).atMost(b)
fun Long.divideUp(l: Long) = (this + l - 1)/l

fun Float.atLeast(f: Float) = max(this, f)
fun Float.atMost(f: Float) = min(this, f)
fun Float.clamp(a: Float, b: Float) = this.atLeast(a).atMost(b)

fun Double.atLeast(d: Double) = max(this, d)
fun Double.atMost(d: Double) = min(this, d)
fun Double.clamp(a: Double, b: Double) = this.atLeast(a).atMost(b)

fun Long.toIntOrNull(): Int? {
	val i = toInt()
	return if (i.toLong() == this) {
		i
	} else {
		null
	}
}

// no idea why Kotlin defines these for integral types, but not floating point types...
val Float.Companion.SIZE_BYTES: Int get() = 4
val Double.Companion.SIZE_BYTES: Int get() = 8

fun Double.sqrt() = Math.sqrt(this)
fun Float.sqrt() = toDouble().sqrt().toFloat()


fun <T> List<T>.indexOfOrNull(thing: T): Int? {
	val index = indexOf(thing)
	if (index == -1) {
		return null
	}
	return index
}
