/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.tools

import org.joml.*
import kotlin.math.max
import kotlin.math.min


val Vector2fc.x get() = x()
val Vector2fc.y get() = y()

val Vector2dc.x get() = x()
val Vector2dc.y get() = y()

fun Vector2dc.toFloat() =
	Vector2f(x.toFloat(), y.toFloat())


val Vector3fc.x get() = x()
val Vector3fc.y get() = y()
val Vector3fc.z get() = z()

val Vector3dc.x get() = x()
val Vector3dc.y get() = y()
val Vector3dc.z get() = z()

fun Vector3dc.toFloat() =
	Vector3f(x.toFloat(), y.toFloat(), z.toFloat())


/** v = v.t/|t| t */
fun Vector3f.parallelTo(target: Vector3f) = apply {
	val scale = this.dot(target)/target.length()
	set(target).mul(scale)
}

/** v -= v.t/|t| t */
fun Vector3f.perpendicularTo(target: Vector3f) = apply {
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
