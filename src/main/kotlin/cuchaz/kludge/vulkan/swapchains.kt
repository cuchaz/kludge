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

	data class SurfaceFormat internal constructor(
		val format: Format,
		val colorSpace: ColorSpace
	) {
		internal constructor(format: VkSurfaceFormatKHR) : this(
			Format.values()[format.format()],
			ColorSpace[format.colorSpace()]
		)
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
	 * Otherwise, the first surface format supported by the surface is used.
	 */
	fun pickSurfaceFormat(format: Format, colorSpace: ColorSpace) =
		if (surfaceFormats.size == 1 && surfaceFormats.get(0).format == Format.UNDEFINED) {
			SurfaceFormat(format, colorSpace)
		} else {
			surfaceFormats
				.find { it.format == Format.B8G8R8A8_UNORM && it.colorSpace == ColorSpace.SRGB_NONLINEAR }
				?: surfaceFormats.get(0)
		}

	enum class PresentMode {
		Immediate,
		Mailbox,
		Fifo,
		FifoRelaxed
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
	 * Picks the first available present mode from the order given.
	 * If no given modes are available, the first available mode is returned.
	 */
	fun pickPresentMode(vararg modes: PresentMode): PresentMode {
		for (mode in modes) {
			if (presentModes.contains(mode)) {
				return mode
			}
		}
		return presentModes.get(0)
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

enum class Format {
	UNDEFINED,
	R4G4_UNORM_PACK8,
	R4G4B4A4_UNORM_PACK16,
	B4G4R4A4_UNORM_PACK16,
	R5G6B5_UNORM_PACK16,
	B5G6R5_UNORM_PACK16,
	R5G5B5A1_UNORM_PACK16,
	B5G5R5A1_UNORM_PACK16,
	A1R5G5B5_UNORM_PACK16,
	R8_UNORM,
	R8_SNORM,
	R8_USCALED,
	R8_SSCALED,
	R8_UINT,
	R8_SINT,
	R8_SRGB,
	R8G8_UNORM,
	R8G8_SNORM,
	R8G8_USCALED,
	R8G8_SSCALED,
	R8G8_UINT,
	R8G8_SINT,
	R8G8_SRGB,
	R8G8B8_UNORM,
	R8G8B8_SNORM,
	R8G8B8_USCALED,
	R8G8B8_SSCALED,
	R8G8B8_UINT,
	R8G8B8_SINT,
	R8G8B8_SRGB,
	B8G8R8_UNORM,
	B8G8R8_SNORM,
	B8G8R8_USCALED,
	B8G8R8_SSCALED,
	B8G8R8_UINT,
	B8G8R8_SINT,
	B8G8R8_SRGB,
	R8G8B8A8_UNORM,
	R8G8B8A8_SNORM,
	R8G8B8A8_USCALED,
	R8G8B8A8_SSCALED,
	R8G8B8A8_UINT,
	R8G8B8A8_SINT,
	R8G8B8A8_SRGB,
	B8G8R8A8_UNORM,
	B8G8R8A8_SNORM,
	B8G8R8A8_USCALED,
	B8G8R8A8_SSCALED,
	B8G8R8A8_UINT,
	B8G8R8A8_SINT,
	B8G8R8A8_SRGB,
	A8B8G8R8_UNORM_PACK32,
	A8B8G8R8_SNORM_PACK32,
	A8B8G8R8_USCALED_PACK32,
	A8B8G8R8_SSCALED_PACK32,
	A8B8G8R8_UINT_PACK32,
	A8B8G8R8_SINT_PACK32,
	A8B8G8R8_SRGB_PACK32,
	A2R10G10B10_UNORM_PACK32,
	A2R10G10B10_SNORM_PACK32,
	A2R10G10B10_USCALED_PACK32,
	A2R10G10B10_SSCALED_PACK32,
	A2R10G10B10_UINT_PACK32,
	A2R10G10B10_SINT_PACK32,
	A2B10G10R10_UNORM_PACK32,
	A2B10G10R10_SNORM_PACK32,
	A2B10G10R10_USCALED_PACK32,
	A2B10G10R10_SSCALED_PACK32,
	A2B10G10R10_UINT_PACK32,
	A2B10G10R10_SINT_PACK32,
	R16_UNORM,
	R16_SNORM,
	R16_USCALED,
	R16_SSCALED,
	R16_UINT,
	R16_SINT,
	R16_SFLOAT,
	R16G16_UNORM,
	R16G16_SNORM,
	R16G16_USCALED,
	R16G16_SSCALED,
	R16G16_UINT,
	R16G16_SINT,
	R16G16_SFLOAT,
	R16G16B16_UNORM,
	R16G16B16_SNORM,
	R16G16B16_USCALED,
	R16G16B16_SSCALED,
	R16G16B16_UINT,
	R16G16B16_SINT,
	R16G16B16_SFLOAT,
	R16G16B16A16_UNORM,
	R16G16B16A16_SNORM,
	R16G16B16A16_USCALED,
	R16G16B16A16_SSCALED,
	R16G16B16A16_UINT,
	R16G16B16A16_SINT,
	R16G16B16A16_SFLOAT,
	R32_UINT,
	R32_SINT,
	R32_SFLOAT,
	R32G32_UINT,
	R32G32_SINT,
	R32G32_SFLOAT,
	R32G32B32_UINT,
	R32G32B32_SINT,
	R32G32B32_SFLOAT,
	R32G32B32A32_UINT,
	R32G32B32A32_SINT,
	R32G32B32A32_SFLOAT,
	R64_UINT,
	R64_SINT,
	R64_SFLOAT,
	R64G64_UINT,
	R64G64_SINT,
	R64G64_SFLOAT,
	R64G64B64_UINT,
	R64G64B64_SINT,
	R64G64B64_SFLOAT,
	R64G64B64A64_UINT,
	R64G64B64A64_SINT,
	R64G64B64A64_SFLOAT,
	B10G11R11_UFLOAT_PACK32,
	E5B9G9R9_UFLOAT_PACK32,
	D16_UNORM,
	X8_D24_UNORM_PACK32,
	D32_SFLOAT,
	S8_UINT,
	D16_UNORM_S8_UINT,
	D24_UNORM_S8_UINT,
	D32_SFLOAT_S8_UINT,
	BC1_RGB_UNORM_BLOCK,
	BC1_RGB_SRGB_BLOCK,
	BC1_RGBA_UNORM_BLOCK,
	BC1_RGBA_SRGB_BLOCK,
	BC2_UNORM_BLOCK,
	BC2_SRGB_BLOCK,
	BC3_UNORM_BLOCK,
	BC3_SRGB_BLOCK,
	BC4_UNORM_BLOCK,
	BC4_SNORM_BLOCK,
	BC5_UNORM_BLOCK,
	BC5_SNORM_BLOCK,
	BC6H_UFLOAT_BLOCK,
	BC6H_SFLOAT_BLOCK,
	BC7_UNORM_BLOCK,
	BC7_SRGB_BLOCK,
	ETC2_R8G8B8_UNORM_BLOCK,
	ETC2_R8G8B8_SRGB_BLOCK,
	ETC2_R8G8B8A1_UNORM_BLOCK,
	ETC2_R8G8B8A1_SRGB_BLOCK,
	ETC2_R8G8B8A8_UNORM_BLOCK,
	ETC2_R8G8B8A8_SRGB_BLOCK,
	EAC_R11_UNORM_BLOCK,
	EAC_R11_SNORM_BLOCK,
	EAC_R11G11_UNORM_BLOCK,
	EAC_R11G11_SNORM_BLOCK,
	ASTC_4x4_UNORM_BLOCK,
	ASTC_4x4_SRGB_BLOCK,
	ASTC_5x4_UNORM_BLOCK,
	ASTC_5x4_SRGB_BLOCK,
	ASTC_5x5_UNORM_BLOCK,
	ASTC_5x5_SRGB_BLOCK,
	ASTC_6x5_UNORM_BLOCK,
	ASTC_6x5_SRGB_BLOCK,
	ASTC_6x6_UNORM_BLOCK,
	ASTC_6x6_SRGB_BLOCK,
	ASTC_8x5_UNORM_BLOCK,
	ASTC_8x5_SRGB_BLOCK,
	ASTC_8x6_UNORM_BLOCK,
	ASTC_8x6_SRGB_BLOCK,
	ASTC_8x8_UNORM_BLOCK,
	ASTC_8x8_SRGB_BLOCK,
	ASTC_10x5_UNORM_BLOCK,
	ASTC_10x5_SRGB_BLOCK,
	ASTC_10x6_UNORM_BLOCK,
	ASTC_10x6_SRGB_BLOCK,
	ASTC_10x8_UNORM_BLOCK,
	ASTC_10x8_SRGB_BLOCK,
	ASTC_10x10_UNORM_BLOCK,
	ASTC_10x10_SRGB_BLOCK,
	ASTC_12x10_UNORM_BLOCK,
	ASTC_12x10_SRGB_BLOCK,
	ASTC_12x12_UNORM_BLOCK,
	ASTC_12x12_SRGB_BLOCK
}

enum class ColorSpace(val value: Int) {

	SRGB_NONLINEAR(0),
	DISPLAY_P3_NONLINEAR(1000104001),
	EXTENDED_SRGB_LINEAR(1000104002),
	EXTENDED_SRGB_NONLINEAR(1000104014),
	DCI_P3_LINEAR(1000104003),
	DCI_P3_NONLINEAR(1000104004),
	BT709_LINEAR(1000104005),
	BT709_NONLINEAR(1000104006),
	BT2020_LINEAR(1000104007),
	HDR10_ST2084(1000104008),
	DOLBYVISION(1000104009),
	HDR10_HLG(1000104010),
	ADOBERGB_LINEAR(1000104011),
	ADOBERGB_NONLINEAR(1000104012),
	PASS_THROUGH(1000104013);

	companion object {
		operator fun get(value: Int) =
			values()
				.find { it.value == value }
				?: throw NoSuchElementException("unknown color space: $value")
	}
}


class Swapchain internal constructor(
	val device: Device,
	internal val id: Long,
	val surfaceFormat: SwapchainSupport.SurfaceFormat,
	val extent: Extent2D
) : AutoCloseable {

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
				.map { Image(device, pImages.get()) }
		}
	}

	override fun close() {
		vkDestroySwapchainKHR(device.vkDevice, id, null)
	}

	fun acquireNextImage(
		semaphore: Semaphore,
		timeoutNs: Long = -1
	): Int {
		memstack { mem ->
			val fence = VK_NULL_HANDLE // TODO: support fences?
			val pIndex = mem.mallocInt(1)
			vkAcquireNextImageKHR(device.vkDevice, id, timeoutNs, semaphore.id, fence, pIndex)
				.orFail("failed to acquire next image")
			return pIndex.get(0) // TODO: hide index from caller
		}
	}
}

fun SwapchainSupport.swapchain(
	device: Device,
	imageCount: Int = capabilities.minImageCount,
	surfaceFormat: SwapchainSupport.SurfaceFormat,
	presentMode: SwapchainSupport.PresentMode,
	extent: Extent2D = pickExtent(),
	arrayLayers: Int = 1,
	usage: IntFlags<ImageUsage> = IntFlags.of(ImageUsage.ColorAttachment),
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
			.imageExtent(extent.toVulkan(mem))
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
		return Swapchain(device, pSwapchain.get(0), surfaceFormat, extent)
	}
}

enum class ImageUsage(override val value: Int) : IntFlags.Bit {
	TransferSrc(VK_IMAGE_USAGE_TRANSFER_SRC_BIT),
	TransferDst(VK_IMAGE_USAGE_TRANSFER_DST_BIT),
	Sampled(VK_IMAGE_USAGE_SAMPLED_BIT),
	Storage(VK_IMAGE_USAGE_STORAGE_BIT),
	ColorAttachment(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT),
	DepthStencilAttachment(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
	TransientAttachment(VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT),
	InputAttachment(VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT);
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
