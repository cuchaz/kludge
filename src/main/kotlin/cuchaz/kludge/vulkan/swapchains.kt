package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.*


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
		val currentTransform: Int,
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
			caps.currentTransform(),
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
