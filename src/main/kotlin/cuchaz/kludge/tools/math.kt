/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import org.joml.*
import kotlin.math.max
import kotlin.math.min


fun Number.toString(decimals: Int? = null, size: Int? = null) =
	"%${size ?: ""}${if (decimals != null) ".$decimals" else ""}f".format(this)

fun List<Number>.toString(decimals: Int? = null, size: Int? = null) =
	"[${joinToString(", ") { it.toString(decimals, size) }}]"

val Vector2fc.x get() = x()
val Vector2fc.y get() = y()

fun Vector2fc.toString(decimals: Int? = null, size: Int? = null) =
	listOf(x, y).toString(decimals, size)

val Vector2dc.x get() = x()
val Vector2dc.y get() = y()

fun Vector2dc.toString(decimals: Int? = null, size: Int? = null) =
	listOf(x, y).toString(decimals, size)

fun Vector2dc.toFloat() =
	Vector2f(x.toFloat(), y.toFloat())


val Vector3fc.x get() = x()
val Vector3fc.y get() = y()
val Vector3fc.z get() = z()

fun Vector3fc.toString(decimals: Int? = null, size: Int? = null) =
	listOf(x, y, z).toString(decimals, size)


val Vector3dc.x get() = x()
val Vector3dc.y get() = y()
val Vector3dc.z get() = z()

fun Vector3dc.toString(decimals: Int? = null, size: Int? = null) =
	listOf(x, y, z).toString(decimals, size)

fun Vector3dc.toFloat() =
	Vector3f(x.toFloat(), y.toFloat(), z.toFloat())


/** v = v.t/|t| t */
fun Vector3f.parallelTo(target: Vector3fc) = apply {
	val scale = this.dot(target)/target.length()
	set(target).mul(scale)
}

/** v -= v.t/|t| t */
fun Vector3f.perpendicularTo(target: Vector3fc) = apply {
	val scale = this.dot(target)/target.length()
	set(
		x - scale*target.x,
		y - scale*target.y,
		z - scale*target.z
	)
}


fun Vector3f.boxMin(box: AABBf) = this
	.set(box.minX, box.minY, box.minZ)

fun Vector3f.boxMax(box: AABBf) = this
	.set(box.maxX, box.maxY, box.maxZ)

fun Vector3f.boxCenter(box: AABBf) = this
	.boxMax(box)
	.add(box.minX, box.minY, box.minZ)
	.mul(0.5f)

val AABBf.numCorners: Int get() = 8

fun Vector3f.boxCorner(box: AABBf, index: Int) =
	when (index) {
		0 -> set(box.minX, box.minY, box.minZ)
		1 -> set(box.minX, box.minY, box.maxZ)
		2 -> set(box.minX, box.maxY, box.minZ)
		3 -> set(box.minX, box.maxY, box.maxZ)
		4 -> set(box.maxX, box.minY, box.minZ)
		5 -> set(box.maxX, box.minY, box.maxZ)
		6 -> set(box.maxX, box.maxY, box.minZ)
		7 -> set(box.maxX, box.maxY, box.maxZ)
		else -> throw IllegalArgumentException("invalid corner: $index")
	}


fun AABBf.expand(f: Float) {
	minX -= f
	minY -= f
	minZ -= f
	maxX += f
	maxY += f
	maxZ += f
}

fun AABBf.expandToInclude(x: Float, y: Float, z: Float) {
	minX = min(minX, x)
	minY = min(minY, y)
	minZ = min(minZ, z)
	maxX = max(maxX, x)
	maxY = max(maxY, y)
	maxZ = max(maxZ, z)
}

fun AABBf.expandToInclude(v: Vector3f) = expandToInclude(v.x, v.y, v.z)

fun AABBd.toFloat() =
	AABBf(
		minX.toFloat(), minY.toFloat(), minZ.toFloat(),
		maxX.toFloat(), maxY.toFloat(), maxZ.toFloat()
	)


val Quaterniondc.x get() = x()
val Quaterniondc.y get() = y()
val Quaterniondc.z get() = z()
val Quaterniondc.w get() = w()

fun Quaterniondc.isFinite() =
	x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()

fun Quaterniondc.toString(decimals: Int? = null, size: Int? = null) =
	listOf(x, y, z, w).toString(decimals, size)


val Quaternionfc.x get() = x()
val Quaternionfc.y get() = y()
val Quaternionfc.z get() = z()
val Quaternionfc.w get() = w()

fun Quaternionfc.isFinite() =
	x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()

fun Quaternionfc.toString(decimals: Int? = null, size: Int? = null) =
	listOf(x, y, z, w).toString(decimals, size)
