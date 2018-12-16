/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import org.lwjgl.vulkan.VK10


class VulkanException(val err: Int, val msg: String? = null) : RuntimeException(
	if (msg != null) {
		"$msg\n${Vulkan.errors[err]}"
	} else {
		Vulkan.errors[err]
	}
)

fun Int.orFail(handler: (Int) -> Nothing) {
	if (this != VK10.VK_SUCCESS) {
		handler(this)
	}
}

fun Int.orFail(msg: String? = null) = orFail { err -> throw VulkanException(err, msg) }

fun Boolean.toVulkan() =
	if (this) {
		VK10.VK_TRUE
	} else {
		VK10.VK_FALSE
	}
