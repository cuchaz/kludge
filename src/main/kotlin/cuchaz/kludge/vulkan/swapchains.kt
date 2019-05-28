/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*


fun PhysicalDevice.swapchainSupport(surface: Surface) =
	SwapchainSupport(this, surface)

enum class PresentMode {
	Immediate,
	Mailbox,
	Fifo,
	FifoRelaxed
}

data class SurfaceFormat internal constructor(
	val format: Image.Format,
	val colorSpace: Image.ColorSpace
) {
	internal constructor(format: VkSurfaceFormatKHR) : this(
		Image.Format.values()[format.format()],
		Image.ColorSpace[format.colorSpace()]
	)
}

class SwapchainSupport internal constructor(
	val physicalDevice: PhysicalDevice,
	val surface: Surface
) {

	data class Capabilities internal constructor(
		val minImageCount: Int,
		val maxImageCount: Int,
		val currentExtent: Extent2D,
		val minImageExtent: Extent2D,
		val maxImageExtent: Extent2D,
		val maxImageArrayLayers: Int,
		val supportedTransforms: Int,
		val currentTransform: IntFlags<Transform>,
		val supportedCompositeAlpha: Int,
		val supportedUsageFlags: Int
	) {
		internal constructor(caps: VkSurfaceCapabilitiesKHR) : this(
			caps.minImageCount(),
			caps.maxImageCount(),
			caps.currentExtent().toExtent2D(),
			caps.minImageExtent().toExtent2D(),
			caps.maxImageExtent().toExtent2D(),
			caps.maxImageArrayLayers(),
			caps.supportedTransforms(),
			IntFlags(caps.currentTransform()),
			caps.supportedCompositeAlpha(),
			caps.supportedUsageFlags()
		)
	}

	val capabilities: Capabilities by lazy {
		memstack { mem ->
			val pCaps = VkSurfaceCapabilitiesKHR.mallocStack(mem)
			vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.vkDevice, surface.id, pCaps)
			Capabilities(pCaps)
		}
	}

	val surfaceFormats: List<SurfaceFormat> by lazy {
		memstack { mem ->
			val pCount = mem.mallocInt(1)
			vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.vkDevice, surface.id, pCount, null)
			val count = pCount.get(0)
			val pFormats = VkSurfaceFormatKHR.mallocStack(count, mem)
			vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.vkDevice, surface.id, pCount, pFormats)
			(0 until count)
				.map { SurfaceFormat(pFormats.get()) }
		}
	}

	/**
	 * Attempts to find a surface format with the desired properties.
	 * If the surface has no preference, a surface format with the desired properties is created.
	 * If the surface has preferences, and the desired properties match, the matching surface format is returned.
	 * Otherwise, null is returned
	 */
	fun find(format: Image.Format, colorSpace: Image.ColorSpace): SurfaceFormat? =
		if (surfaceFormats.size == 1 && surfaceFormats.get(0).format == Image.Format.UNDEFINED) {
			SurfaceFormat(format, colorSpace)
		} else {
			surfaceFormats
				.find { it.format == Image.Format.B8G8R8A8_UNORM && it.colorSpace == Image.ColorSpace.SRGB_NONLINEAR }
		}

	val presentModes: List<PresentMode> by lazy {
		memstack { mem ->
			val pCount = mem.mallocInt(1)
			vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.vkDevice, surface.id, pCount, null)
			val count = pCount.get(0)
			val pModes = mem.mallocInt(count)
			vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.vkDevice, surface.id, pCount, pModes)
			(0 until count)
				.map { PresentMode.values()[pModes.get()] }
		}
	}

	/**
	 * Attempts to find the present mode.
	 * If the present mode is supported, that present mode is returned.
	 * Otherwise, null is returned.
	 */
	fun find(mode: PresentMode): PresentMode? {
		if (presentModes.contains(mode)) {
			return mode
		}
		return null
	}

	/**
	 * Returns the current window extents, unless the window manager doesn't report useful window extents
	 * In that case, returns the minimum window extents instead
	 */
	fun pickExtent(): Extent2D {

		// some window managers send a bogus value to indicate we should explicitly pick an extent
		// NOTE: uint32_t max would be interpreted as a signed integer with value -1 in the JVM
		if (capabilities.currentExtent.width < 0) {
			return capabilities.minImageExtent
		}

		// otherwise, use the current extent
		return capabilities.currentExtent
	}
}

class Swapchain internal constructor(
	val device: Device,
	internal val id: Long,
	val surfaceFormat: SurfaceFormat,
	val presentMode: PresentMode,
	val extent: Extent2D,
	val usage: IntFlags<Image.Usage>
) : AutoCloseable {

	override fun toString() = "0x%x".format(id)

	val rect: Rect2D = Rect2D(Offset2D(0, 0), extent)
	val viewport: Viewport = Viewport(
		0.0f,
		0.0f,
		extent.width.toFloat(),
		extent.height.toFloat()
	)

	// NOTE: these images do not need explicit cleanup
	val images: List<Image> by lazy {
		memstack { mem ->
			val pCount = mem.mallocInt(1)
			vkGetSwapchainImagesKHR(device.vkDevice, id, pCount, null)
			val count = pCount.get(0)
			val pImages = mem.mallocLong(count)
			vkGetSwapchainImagesKHR(device.vkDevice, id, pCount, pImages)
				.orFail("failed to get swapchain images")
			(0 until count)
				.map { Image(device, pImages.get(), Image.Type.TwoD, extent.to3D(1), surfaceFormat.format, usage) }
		}
	}

	override fun close() {
		vkDestroySwapchainKHR(device.vkDevice, id, null)
	}

	/** will throw SwapchainOutOfDateException if the swapchain can no longer disiplay frames */
	fun acquireNextImage(
		semaphore: Semaphore,
		timeoutNs: Long = -1
	): Int {
		memstack { mem ->
			val fence = VK_NULL_HANDLE // TODO: support fences?
			val pIndex = mem.mallocInt(1)
			vkAcquireNextImageKHR(device.vkDevice, id, timeoutNs, semaphore.id, fence, pIndex)
				.orFailWhen(VK_ERROR_OUT_OF_DATE_KHR) {
					// convert error to exception, but make sure caller can recognize it and catch it
					throw SwapchainOutOfDateException()
				}
				.orFail("failed to acquire next image")
			return pIndex.get(0) // TODO: hide index from caller
		}
	}
}

fun SwapchainSupport.swapchain(
	device: Device,
	imageCount: Int = capabilities.minImageCount,
	surfaceFormat: SurfaceFormat,
	presentMode: PresentMode,
	extent: Extent2D = pickExtent(),
	arrayLayers: Int = 1,
	usage: IntFlags<Image.Usage> = IntFlags.of(Image.Usage.ColorAttachment),
	concurrentQueues: Set<PhysicalDevice.QueueFamily> = emptySet(),
	transform: IntFlags<Transform> = capabilities.currentTransform,
	compositeAlpha: IntFlags<CompositeAlpha> = IntFlags.of(CompositeAlpha.Opaque),
	clipped: Boolean = true,
	oldSwapchain: Swapchain? = null
): Swapchain {
	memstack { mem ->

		val info = VkSwapchainCreateInfoKHR.callocStack(mem)
			.sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
			.surface(surface.id)
			.minImageCount(imageCount)
			.imageFormat(surfaceFormat.format.ordinal)
			.imageColorSpace(surfaceFormat.colorSpace.value)
			.imageExtent { it.set(extent) }
			.imageArrayLayers(arrayLayers)
			.imageUsage(usage.value)
			.preTransform(transform.value)
			.compositeAlpha(compositeAlpha.value)
			.presentMode(presentMode.ordinal)
			.clipped(clipped)
			.oldSwapchain(oldSwapchain?.id ?: VK_NULL_HANDLE)

		if (concurrentQueues.size > 1) {
			info.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
			info.pQueueFamilyIndices(concurrentQueues.map { it.index }.toBuffer(mem))
		} else {
			info.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
		}

		val pSwapchain = mem.mallocLong(1)
		vkCreateSwapchainKHR(device.vkDevice, info, null, pSwapchain)
			.orFail("failed to create swapchain")
		return Swapchain(device, pSwapchain.get(0), surfaceFormat, presentMode, extent, usage)
	}
}

enum class Transform(override val value: Int) : IntFlags.Bit {
	Identity(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR),
	Rotate90(VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR),
	Rotate180(VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR),
	Rotate270(VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR),
	HorizontalMirror(VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_BIT_KHR),
	HorizontalMirrorRotate90(VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_90_BIT_KHR),
	HorizontalMirrorRotate180(VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_180_BIT_KHR),
	HorizontalMirrorRotate270(VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_270_BIT_KHR),
	Inherit(VK_SURFACE_TRANSFORM_INHERIT_BIT_KHR);
}

enum class CompositeAlpha(override val value: Int) : IntFlags.Bit {
	Opaque(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR),
	PreMultiplied(VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR),
	PostMultiplied(VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR),
	Inherit(VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR);
}


class SwapchainOutOfDateException : Exception("Swapchain is out of date and must be recreated")
