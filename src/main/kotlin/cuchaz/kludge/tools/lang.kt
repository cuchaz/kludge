/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import java.lang.Math.*


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

fun Double.sqrt() = sqrt(this)
fun Float.sqrt() = toDouble().sqrt().toFloat()

fun Double.square() = this*this
fun Float.square() = this*this

fun Double.toDegrees() = toDegrees(this)
fun Float.toDegrees() = toDegrees(this.toDouble()).toFloat()

fun Double.toRadians() = toRadians(this)
fun Float.toRadians() = toRadians(this.toDouble()).toFloat()


fun <T> List<T>.indexOfOrNull(thing: T): Int? {
	val index = indexOf(thing)
	if (index == -1) {
		return null
	}
	return index
}


fun <K,V> Collection<K>.diff(target: Map<K,V>, added: (K) -> Unit, removed: (K, V) -> Unit) {

	val source = this

	// detect removals
	target.entries
		.filter { (key, _) -> key !in source }
		.forEach { (key, value) -> removed(key, value) }

	// detect additions
	source
		.filter { key -> key !in target }
		.forEach { key -> added(key) }
}


fun <K> Collection<K>.diff(target: Set<K>, added: (K) -> Unit, removed: (K) -> Unit) {

	val source = this

	// detect removals
	target
		.filter { it !in source }
		.forEach { removed(it) }

	// detect additions
	source
		.filter { it !in target }
		.forEach { added(it) }
}

fun <K> Collection<K>.changed(target: Set<K>): Boolean {

	val source = this

	// look for easy outs first
	if (source.size != target.size) {
		return true
	}

	// detect replacements
	for (it in source) {
		if (it !in target) {
			return true
		}
	}

	return false
}
