/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK11.*


class Image internal constructor(
	val device: Device,
	internal val id: Long,
	val type: Image.Type,
	val extent: Extent3D,
	val format: Image.Format,
	val usage: IntFlags<Image.Usage>
) : AutoCloseable {

	override fun toString() = "0x%x".format(id)

	override fun close() {
		vkDestroyImage(device.vkDevice, id, null)
	}

	inner class View internal constructor(
		internal val id: Long
	) : AutoCloseable {

		override fun toString() = "0x%x".format(id)

		val image: Image = this@Image

		override fun close() {
			vkDestroyImageView(device.vkDevice, id, null)
		}
	}

	enum class Type {

		OneD,
		TwoD,
		ThreeD;

		fun toViewType() =
			when (this) {
				OneD -> ViewType.OneD
				TwoD -> ViewType.TwoD
				ThreeD -> ViewType.ThreeD
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

	enum class Create(override val value: Int) : IntFlags.Bit {
		SparseBinding(VK_IMAGE_CREATE_SPARSE_BINDING_BIT),
		SparseResidency(VK_IMAGE_CREATE_SPARSE_RESIDENCY_BIT),
		SparseAliased(VK_IMAGE_CREATE_SPARSE_ALIASED_BIT),
		MutableFormat(VK_IMAGE_CREATE_MUTABLE_FORMAT_BIT),
		CubeCompatible(VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT),
		Alias(VK_IMAGE_CREATE_ALIAS_BIT),
		SplitInstanceBindRegions(VK_IMAGE_CREATE_SPLIT_INSTANCE_BIND_REGIONS_BIT),
		TwoDArrayCompatible(VK_IMAGE_CREATE_2D_ARRAY_COMPATIBLE_BIT),
		BlockTexelViewCompatible(VK_IMAGE_CREATE_BLOCK_TEXEL_VIEW_COMPATIBLE_BIT),
		ExtendedUsage(VK_IMAGE_CREATE_EXTENDED_USAGE_BIT),
		Projected(VK_IMAGE_CREATE_PROTECTED_BIT),
		Disjoint(VK_IMAGE_CREATE_DISJOINT_BIT)
	}

	enum class Tiling {
		Optimal,
		Linear
	}

	enum class Layout(val value: Int) {
		Undefined(VK_IMAGE_LAYOUT_UNDEFINED),
		General(VK_IMAGE_LAYOUT_GENERAL),
		ColorAttachmentOptimal(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL),
		DepthStencilAttachmentOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL),
		DepthStencilReadOnlyOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL),
		ShaderReadOnlyOptimal(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
		TransferSrcOptimal(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL),
		TransferDstOptimal(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL),
		Preinitialized(VK_IMAGE_LAYOUT_PREINITIALIZED),
		PresentSrc(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
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

	enum class Usage(override val value: Int) : IntFlags.Bit {
		TransferSrc(VK_IMAGE_USAGE_TRANSFER_SRC_BIT),
		TransferDst(VK_IMAGE_USAGE_TRANSFER_DST_BIT),
		Sampled(VK_IMAGE_USAGE_SAMPLED_BIT),
		Storage(VK_IMAGE_USAGE_STORAGE_BIT),
		ColorAttachment(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT),
		DepthStencilAttachment(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
		TransientAttachment(VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT),
		InputAttachment(VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT)
	}

	val memoryRequirements: MemoryRequirements by lazy {
		memstack { mem ->
			val pMem = VkMemoryRequirements.mallocStack(mem)
			vkGetImageMemoryRequirements(device.vkDevice, id, pMem)
			MemoryRequirements(
				device.physicalDevice,
				pMem.size(),
				pMem.alignment(),
				pMem.memoryTypeBits()
			)
		}
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

	data class SubresourceRange(
		val aspectMask: IntFlags<Aspect> = IntFlags.of(Aspect.Color),
		val baseMipLevel: Int = 0,
		val levelCount: Int = 1,
		val baseArrayLayer: Int = 0,
		val layerCount: Int = 1
	)

	data class SubresourceLayers(
		val aspectMask: IntFlags<Aspect> = IntFlags.of(Aspect.Color),
		val mipLevel: Int = 0,
		val baseArrayLayer: Int = 0,
		val layerCount: Int = 1
	)

	fun view(
		viewType: ViewType = this.type.toViewType(),
		format: Format = this.format,
		components: Components = Components(),
		range: SubresourceRange = SubresourceRange()
	): View {
		memstack { mem ->
			val info = VkImageViewCreateInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
				.flags(0) // reserved for future use
				.image(id)
				.viewType(viewType.ordinal)
				.format(format.ordinal)
				.components { it.set(components) }
				.subresourceRange { it.set(range) }
			val pView = mem.mallocLong(1)
			vkCreateImageView(device.vkDevice, info, null, pView)
				.orFail("failed to create image view")
			return View(pView.get(0))
		}
	}

	fun bindTo(mem: MemoryAllocation, offset: Long = 0L) =
		device.bindImageMemory(this, mem, offset)

	inner class Allocated(
		val memory: MemoryAllocation
	) : AutoCloseable {

		val image: Image = this@Image

		override fun close() {
			memory.close()
		}
	}

	fun allocate(memType: MemoryType): Allocated {
		val mem = device.allocateMemory(memoryRequirements.size, memType)
		bindTo(mem)
		return Allocated(mem)
	}

	fun allocate(memTypeFilter: (MemoryType) -> Boolean) =
		allocate(memoryRequirements.memoryTypes
			.firstOrNull(memTypeFilter)
			?: throw NoSuchElementException("no suitable memory type")
		)

	data class Subresource(
		val aspectMask: IntFlags<Aspect> = IntFlags.of(Aspect.Color),
		val mipLevel: Int = 0,
		val arrayLayer: Int = 0
	)

	data class SubresourceLayout(
		val offset: Long,
		val size: Long,
		val rowPitch: Long,
		val arrayPitch: Long,
		val depthPitch: Long
	)

	fun getSubresourceLayout(subresource: Subresource = Subresource()): SubresourceLayout {
		memstack { mem ->
			val pSubresource = VkImageSubresource.callocStack(mem)
				.aspectMask(subresource.aspectMask.value)
				.mipLevel(subresource.mipLevel)
				.arrayLayer(subresource.arrayLayer)
			val pLayout = VkSubresourceLayout.mallocStack(mem)
			vkGetImageSubresourceLayout(device.vkDevice, id, pSubresource, pLayout)
			return SubresourceLayout(
				pLayout.offset(),
				pLayout.size(),
				pLayout.rowPitch(),
				pLayout.arrayPitch(),
				pLayout.depthPitch()
			)
		}
	}
}

fun Device.image(
	type: Image.Type,
	extent: Extent3D,
	format: Image.Format,
	usage: IntFlags<Image.Usage>,
	flags: IntFlags<Image.Create> = IntFlags(0),
	mipLevels: Int = 1,
	arrayLayers: Int = 1,
	tiling: Image.Tiling = Image.Tiling.Optimal,
	initialLayout: Image.Layout = Image.Layout.Undefined,
	concurrentQueues: Set<PhysicalDevice.QueueFamily> = emptySet(),
	samples: SampleCount = SampleCount.One
): Image {
	memstack { mem ->

		val info = VkImageCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
			.flags(flags.value)
			.imageType(type.ordinal)
			.extent { it.set(extent) }
			.format(format.ordinal)
			.usage(usage.value)
			.mipLevels(mipLevels)
			.arrayLayers(arrayLayers)
			.tiling(tiling.ordinal)
			.initialLayout(initialLayout.ordinal)
			.samples(samples.value)
		if (concurrentQueues.size > 1) {
			info.sharingMode(VK_SHARING_MODE_CONCURRENT)
			info.pQueueFamilyIndices(concurrentQueues.map { it.index }.toBuffer(mem))
		} else {
			info.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
		}

		val pImg = mem.mallocLong(1)
		vkCreateImage(vkDevice, info, null, pImg)
			.orFail("failed to create image")
		return Image(this, pImg.get(0), type, extent, format, usage)
	}
}

fun Device.bindImageMemory(image: Image, mem: MemoryAllocation, offset: Long = 0L) {
	vkBindImageMemory(vkDevice, image.id, mem.id, offset)
		.orFail("failed to bind image to device memory")
}

internal fun VkComponentMapping.set(src: Image.Components) {
	r(src.r.ordinal)
	g(src.g.ordinal)
	b(src.b.ordinal)
	a(src.a.ordinal)
}

internal fun VkImageSubresourceRange.set(src: Image.SubresourceRange) {
	aspectMask(src.aspectMask.value)
	baseMipLevel(src.baseMipLevel)
	levelCount(src.levelCount)
	baseArrayLayer(src.baseArrayLayer)
	layerCount(src.layerCount)
}

internal fun VkImageSubresourceLayers.set(src: Image.SubresourceLayers) {
	aspectMask(src.aspectMask.value)
	mipLevel(src.mipLevel)
	baseArrayLayer(src.baseArrayLayer)
	layerCount(src.layerCount)
}
