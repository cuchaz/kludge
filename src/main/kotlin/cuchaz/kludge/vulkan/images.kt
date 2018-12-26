/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkImageViewCreateInfo


class Image internal constructor(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		vkDestroyImage(device.vkDevice, id, null)
	}

	inner class View internal constructor(
		internal val id: Long
	) : AutoCloseable {

		val image: Image = this@Image

		override fun close() {
			vkDestroyImageView(device.vkDevice, id, null)
		}
	}

	enum class ViewType {
		OneD,
		TwoD,
		ThreeD,
		Cube,
		OneDArray,
		TwoDArray,
		CubeArray
	}

	enum class Swizzle {
		Identity,
		Zero,
		One,
		R,
		G,
		B,
		A
	}

	class Components(
		val r: Swizzle = Swizzle.Identity,
		val g: Swizzle = Swizzle.Identity,
		val b: Swizzle = Swizzle.Identity,
		val a: Swizzle = Swizzle.Identity
	)

	enum class Aspect(override val value: Int) : IntFlags.Bit {
		Color(VK_IMAGE_ASPECT_COLOR_BIT),
		Depth(VK_IMAGE_ASPECT_DEPTH_BIT),
		Stencil(VK_IMAGE_ASPECT_STENCIL_BIT),
		Metadata(VK_IMAGE_ASPECT_METADATA_BIT)
	}

	class SubresourceRange(
		val aspectMask: IntFlags<Aspect> = IntFlags.of(Aspect.Color),
		val baseMipLevel: Int = 0,
		val levelCount: Int = 1,
		val baseArrayLayer: Int = 0,
		val layerCount: Int = 1
	)

	fun view(
		viewType: ViewType,
		format: Format,
		components: Components = Components(),
		subresourceRange: SubresourceRange = SubresourceRange()
	): View {
		memstack { mem ->
			val info = VkImageViewCreateInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
				.image(id)
				.viewType(viewType.ordinal)
				.format(format.ordinal)
				.apply {
					components().apply {
						r(components.r.ordinal)
						g(components.g.ordinal)
						b(components.b.ordinal)
						a(components.a.ordinal)
					}
					subresourceRange().apply {
						aspectMask(subresourceRange.aspectMask.value)
						baseMipLevel(subresourceRange.baseMipLevel)
						levelCount(subresourceRange.levelCount)
						baseArrayLayer(subresourceRange.baseArrayLayer)
						layerCount(subresourceRange.layerCount)
					}
				}
			val pView = mem.mallocLong(1)
			vkCreateImageView(device.vkDevice, info, null, pView)
				.orFail("failed to create image view")
			return View(pView.get(0))
		}
	}
}
