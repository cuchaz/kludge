/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class VulkanException(val err: Int, val msg: String? = null) : RuntimeException(
	if (msg != null) {
		"$msg\n${Vulkan.errors[err]}"
	} else {
		Vulkan.errors[err]
	}
)

fun Int.orFail(handler: (Int) -> Nothing) {
	if (this != VK_SUCCESS) {
		handler(this)
	}
}

fun Int.orFail(msg: String? = null) = orFail { err -> throw VulkanException(err, msg) }

fun Boolean.toVulkan() =
	if (this) {
		VK_TRUE
	} else {
		VK_FALSE
	}


data class Version(
	val major: Int,
	val minor: Int,
	val patch: Int = 0
) {

	internal constructor (value: Int) : this(
		VK_VERSION_MAJOR(value),
		VK_VERSION_MINOR(value),
		VK_VERSION_PATCH(value)
	)

	internal val value = VK_MAKE_VERSION(major, minor, patch)

	override fun toString() = "$major.$minor.$patch"
}

data class Extent2D(
	val width: Int,
	val height: Int
) {
	fun to3D(depth: Int) = Extent3D(width, height, depth)
}
internal fun VkExtent2D.set(extent: Extent2D) =
	apply {
		width(extent.width)
		height(extent.height)
	}
internal fun VkExtent2D.toExtent2D() = Extent2D(width(), height())

data class Extent3D(
	val width: Int,
	val height: Int,
	val depth: Int
)
internal fun VkExtent3D.set(extent: Extent3D) =
	apply {
		width(extent.width)
		height(extent.height)
		depth(extent.depth)
	}
internal fun VkExtent3D.toExtent3D() = Extent3D(width(), height(), depth())

data class Offset2D(
	val x: Int,
	val y: Int
) {
	fun to3D(z: Int) = Offset3D(x, y, z)
}
internal fun VkOffset2D.set(offset: Offset2D) =
	apply {
		x(offset.x)
		y(offset.y)
	}
internal fun VkOffset2D.toOffset2D() = Offset2D(x(), y())

data class Offset3D(
	val x: Int,
	val y: Int,
	val z: Int
)
internal fun VkOffset3D.set(offset: Offset3D) =
	apply {
		x(offset.x)
		y(offset.y)
		z(offset.z)
	}
internal fun VkOffset3D.toOffset3D() = Offset3D(x(), y(), z())

data class Rect2D(
	val offset: Offset2D,
	val extent: Extent2D
)
internal fun VkRect2D.set(rect: Rect2D) =
	apply {
		offset().set(rect.offset)
		extent().set(rect.extent)
	}
internal fun VkRect2D.toRect2D() = Rect2D(offset().toOffset2D(), extent().toExtent2D())
