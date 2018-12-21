/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkExtent3D


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

data class Extent3D(
	val width: Int,
	val height: Int,
	val depth: Int
)

fun VkExtent3D.toExtent3D() = Extent3D(width(), height(), depth())
