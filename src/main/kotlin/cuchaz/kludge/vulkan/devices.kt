/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK11.*
import java.util.*


class PhysicalDevice internal constructor (internal val instance: VkInstance, internal val id: Long) {

	internal val vkDevice = VkPhysicalDevice(id, instance)

	enum class Type {

		// NOTE: must match order of eg VK_PHYSICAL_DEVICE_TYPE_OTHER
		Other,
		IntegratedGpu,
		DiscreteGpu,
		VirtualGpu,
		Cpu;

		companion object {
			operator fun get(id: Int) = values()[id]
		}
	}

	companion object {
		const val SwapchainExtension = VK_KHR_SWAPCHAIN_EXTENSION_NAME
	}

	data class Limits internal constructor(
		val maxImageDimension1D: Int,
		val maxImageDimension2D: Int,
		val maxImageDimension3D: Int,
		val maxImageDimensionCube: Int,
		val maxImageArrayLayers: Int,
		val maxTexelBufferElements: Int,
		val maxUniformBufferRange: Int,
		val maxStorageBufferRange: Int,
		val maxPushConstantsSize: Int,
		val maxMemoryAllocationCount: Int,
		val maxSamplerAllocationCount: Int,
		val bufferImageGranularity: Long,
		val sparseAddressSpaceSize: Long,
		val maxBoundDescriptorSets: Int,
		val maxPerStageDescriptorSamplers: Int,
		val maxPerStageDescriptorUniformBuffers: Int,
		val maxPerStageDescriptorStorageBuffers: Int,
		val maxPerStageDescriptorSampledImages: Int,
		val maxPerStageDescriptorStorageImages: Int,
		val maxPerStageDescriptorInputAttachments: Int,
		val maxPerStageResources: Int,
		val maxDescriptorSetSamplers: Int,
		val maxDescriptorSetUniformBuffers: Int,
		val maxDescriptorSetUniformBuffersDynamic: Int,
		val maxDescriptorSetStorageBuffers: Int,
		val maxDescriptorSetStorageBuffersDynamic: Int,
		val maxDescriptorSetSampledImages: Int,
		val maxDescriptorSetStorageImages: Int,
		val maxDescriptorSetInputAttachments: Int,
		val maxVertexInputAttributes: Int,
		val maxVertexInputBindings: Int,
		val maxVertexInputAttributeOffset: Int,
		val maxVertexInputBindingStride: Int,
		val maxVertexOutputComponents: Int,
		val maxTessellationGenerationLevel: Int,
		val maxTessellationPatchSize: Int,
		val maxTessellationControlPerVertexInputComponents: Int,
		val maxTessellationControlPerVertexOutputComponents: Int,
		val maxTessellationControlPerPatchOutputComponents: Int,
		val maxTessellationControlTotalOutputComponents: Int,
		val maxTessellationEvaluationInputComponents: Int,
		val maxTessellationEvaluationOutputComponents: Int,
		val maxGeometryShaderInvocations: Int,
		val maxGeometryInputComponents: Int,
		val maxGeometryOutputComponents: Int,
		val maxGeometryOutputVertices: Int,
		val maxGeometryTotalOutputComponents: Int,
		val maxFragmentInputComponents: Int,
		val maxFragmentOutputAttachments: Int,
		val maxFragmentDualSrcAttachments: Int,
		val maxFragmentCombinedOutputResources: Int,
		val maxComputeSharedMemorySize: Int,
		val maxComputeWorkGroupCount: List<Int>,
		val maxComputeWorkGroupInvocations: Int,
		val maxComputeWorkGroupSize: List<Int>,
		val subPixelPrecisionBits: Int,
		val subTexelPrecisionBits: Int,
		val mipmapPrecisionBits: Int,
		val maxDrawIndexedIndexValue: Int,
		val maxDrawIndirectCount: Int,
		val maxSamplerLodBias: Float,
		val maxSamplerAnisotropy: Float,
		val maxViewports: Int,
		val maxViewportDimensions: List<Int>,
		val viewportBoundsRange: List<Float>,
		val viewportSubPixelBits: Int,
		val minMemoryMapAlignment: Long,
		val minTexelBufferOffsetAlignment: Long,
		val minUniformBufferOffsetAlignment: Long,
		val minStorageBufferOffsetAlignment: Long,
		val minTexelOffset: Int,
		val maxTexelOffset: Int,
		val minTexelGatherOffset: Int,
		val maxTexelGatherOffset: Int,
		val minInterpolationOffset: Float,
		val maxInterpolationOffset: Float,
		val subPixelInterpolationOffsetBits: Int,
		val maxFramebufferWidth: Int,
		val maxFramebufferHeight: Int,
		val maxFramebufferLayers: Int,
		val framebufferColorSampleCounts: Int,
		val framebufferDepthSampleCounts: Int,
		val framebufferStencilSampleCounts: Int,
		val framebufferNoAttachmentsSampleCounts: Int,
		val maxColorAttachments: Int,
		val sampledImageColorSampleCounts: Int,
		val sampledImageIntegerSampleCounts: Int,
		val sampledImageDepthSampleCounts: Int,
		val sampledImageStencilSampleCounts: Int,
		val storageImageSampleCounts: Int,
		val maxSampleMaskWords: Int,
		val timestampComputeAndGraphics: Boolean,
		val timestampPeriod: Float,
		val maxClipDistances: Int,
		val maxCullDistances: Int,
		val maxCombinedClipAndCullDistances: Int,
		val discreteQueuePriorities: Int,
		val pointSizeRange: List<Float>,
		val lineWidthRange: List<Float>,
		val pointSizeGranularity: Float,
		val lineWidthGranularity: Float,
		val strictLines: Boolean,
		val standardSampleLocations: Boolean,
		val optimalBufferCopyOffsetAlignment: Long,
		val optimalBufferCopyRowPitchAlignment: Long,
		val nonCoherentAtomSize: Long
	)

	data class Properties internal constructor(
		val apiVersion: Version,
		val driverVersion: Version,
		val vendorId: Int,
		val deviceId: Int,
		val type: Type,
		val name: String,
		val uuid: UUID,
		val limits: Limits
	)

	val properties: Properties by lazy {
		memstack { mem ->

			val props = VkPhysicalDeviceProperties.mallocStack(mem)
			vkGetPhysicalDeviceProperties(vkDevice, props)

			val limits = props.limits()

			Properties(
				Version(props.apiVersion()),
				Version(props.driverVersion()),
				props.vendorID(),
				props.deviceID(),
				Type[props.deviceType()],
				props.deviceNameString(),
				props.pipelineCacheUUID().toUUID(),
				Limits(
					limits.maxImageDimension1D(),
					limits.maxImageDimension2D(),
					limits.maxImageDimension3D(),
					limits.maxImageDimensionCube(),
					limits.maxImageArrayLayers(),
					limits.maxTexelBufferElements(),
					limits.maxUniformBufferRange(),
					limits.maxStorageBufferRange(),
					limits.maxPushConstantsSize(),
					limits.maxMemoryAllocationCount(),
					limits.maxSamplerAllocationCount(),
					limits.bufferImageGranularity(),
					limits.sparseAddressSpaceSize(),
					limits.maxBoundDescriptorSets(),
					limits.maxPerStageDescriptorSamplers(),
					limits.maxPerStageDescriptorUniformBuffers(),
					limits.maxPerStageDescriptorStorageBuffers(),
					limits.maxPerStageDescriptorSampledImages(),
					limits.maxPerStageDescriptorStorageImages(),
					limits.maxPerStageDescriptorInputAttachments(),
					limits.maxPerStageResources(),
					limits.maxDescriptorSetSamplers(),
					limits.maxDescriptorSetUniformBuffers(),
					limits.maxDescriptorSetUniformBuffersDynamic(),
					limits.maxDescriptorSetStorageBuffers(),
					limits.maxDescriptorSetStorageBuffersDynamic(),
					limits.maxDescriptorSetSampledImages(),
					limits.maxDescriptorSetStorageImages(),
					limits.maxDescriptorSetInputAttachments(),
					limits.maxVertexInputAttributes(),
					limits.maxVertexInputBindings(),
					limits.maxVertexInputAttributeOffset(),
					limits.maxVertexInputBindingStride(),
					limits.maxVertexOutputComponents(),
					limits.maxTessellationGenerationLevel(),
					limits.maxTessellationPatchSize(),
					limits.maxTessellationControlPerVertexInputComponents(),
					limits.maxTessellationControlPerVertexOutputComponents(),
					limits.maxTessellationControlPerPatchOutputComponents(),
					limits.maxTessellationControlTotalOutputComponents(),
					limits.maxTessellationEvaluationInputComponents(),
					limits.maxTessellationEvaluationOutputComponents(),
					limits.maxGeometryShaderInvocations(),
					limits.maxGeometryInputComponents(),
					limits.maxGeometryOutputComponents(),
					limits.maxGeometryOutputVertices(),
					limits.maxGeometryTotalOutputComponents(),
					limits.maxFragmentInputComponents(),
					limits.maxFragmentOutputAttachments(),
					limits.maxFragmentDualSrcAttachments(),
					limits.maxFragmentCombinedOutputResources(),
					limits.maxComputeSharedMemorySize(),
					limits.maxComputeWorkGroupCount().toList(3),
					limits.maxComputeWorkGroupInvocations(),
					limits.maxComputeWorkGroupSize().toList(3),
					limits.subPixelPrecisionBits(),
					limits.subTexelPrecisionBits(),
					limits.mipmapPrecisionBits(),
					limits.maxDrawIndexedIndexValue(),
					limits.maxDrawIndirectCount(),
					limits.maxSamplerLodBias(),
					limits.maxSamplerAnisotropy(),
					limits.maxViewports(),
					limits.maxViewportDimensions().toList(2),
					limits.viewportBoundsRange().toList(2),
					limits.viewportSubPixelBits(),
					limits.minMemoryMapAlignment(),
					limits.minTexelBufferOffsetAlignment(),
					limits.minUniformBufferOffsetAlignment(),
					limits.minStorageBufferOffsetAlignment(),
					limits.minTexelOffset(),
					limits.maxTexelOffset(),
					limits.minTexelGatherOffset(),
					limits.maxTexelGatherOffset(),
					limits.minInterpolationOffset(),
					limits.maxInterpolationOffset(),
					limits.subPixelInterpolationOffsetBits(),
					limits.maxFramebufferWidth(),
					limits.maxFramebufferHeight(),
					limits.maxFramebufferLayers(),
					limits.framebufferColorSampleCounts(),
					limits.framebufferDepthSampleCounts(),
					limits.framebufferStencilSampleCounts(),
					limits.framebufferNoAttachmentsSampleCounts(),
					limits.maxColorAttachments(),
					limits.sampledImageColorSampleCounts(),
					limits.sampledImageIntegerSampleCounts(),
					limits.sampledImageDepthSampleCounts(),
					limits.sampledImageStencilSampleCounts(),
					limits.storageImageSampleCounts(),
					limits.maxSampleMaskWords(),
					limits.timestampComputeAndGraphics(),
					limits.timestampPeriod(),
					limits.maxClipDistances(),
					limits.maxCullDistances(),
					limits.maxCombinedClipAndCullDistances(),
					limits.discreteQueuePriorities(),
					limits.pointSizeRange().toList(2),
					limits.lineWidthRange().toList(2),
					limits.pointSizeGranularity(),
					limits.lineWidthGranularity(),
					limits.strictLines(),
					limits.standardSampleLocations(),
					limits.optimalBufferCopyOffsetAlignment(),
					limits.optimalBufferCopyRowPitchAlignment(),
					limits.nonCoherentAtomSize()
				)
			)
		}
	}

	data class Features(
		var robustBufferAccess: Boolean = false,
		var fullDrawIndexUint32: Boolean = false,
		var imageCubeArray: Boolean = false,
		var independentBlend: Boolean = false,
		var geometryShader: Boolean = false,
		var tessellationShader: Boolean = false,
		var sampleRateShading: Boolean = false,
		var dualSrcBlend: Boolean = false,
		var logicOp: Boolean = false,
		var multiDrawIndirect: Boolean = false,
		var drawIndirectFirstInstance: Boolean = false,
		var depthClamp: Boolean = false,
		var depthBiasClamp: Boolean = false,
		var fillModeNonSolid: Boolean = false,
		var depthBounds: Boolean = false,
		var wideLines: Boolean = false,
		var largePoints: Boolean = false,
		var alphaToOne: Boolean = false,
		var multiViewport: Boolean = false,
		var samplerAnisotropy: Boolean = false,
		var textureCompressionETC2: Boolean = false,
		var textureCompressionASTC_LDR: Boolean = false,
		var textureCompressionBC: Boolean = false,
		var occlusionQueryPrecise: Boolean = false,
		var pipelineStatisticsQuery: Boolean = false,
		var vertexPipelineStoresAndAtomics: Boolean = false,
		var fragmentStoresAndAtomics: Boolean = false,
		var shaderTessellationAndGeometryPointSize: Boolean = false,
		var shaderImageGatherExtended: Boolean = false,
		var shaderStorageImageExtendedFormats: Boolean = false,
		var shaderStorageImageMultisample: Boolean = false,
		var shaderStorageImageReadWithoutFormat: Boolean = false,
		var shaderStorageImageWriteWithoutFormat: Boolean = false,
		var shaderUniformBufferArrayDynamicIndexing: Boolean = false,
		var shaderSampledImageArrayDynamicIndexing: Boolean = false,
		var shaderStorageBufferArrayDynamicIndexing: Boolean = false,
		var shaderStorageImageArrayDynamicIndexing: Boolean = false,
		var shaderClipDistance: Boolean = false,
		var shaderCullDistance: Boolean = false,
		var shaderFloat64: Boolean = false,
		var shaderInt64: Boolean = false,
		var shaderInt16: Boolean = false,
		var shaderResourceResidency: Boolean = false,
		var shaderResourceMinLod: Boolean = false,
		var sparseBinding: Boolean = false,
		var sparseResidencyBuffer: Boolean = false,
		var sparseResidencyImage2D: Boolean = false,
		var sparseResidencyImage3D: Boolean = false,
		var sparseResidency2Samples: Boolean = false,
		var sparseResidency4Samples: Boolean = false,
		var sparseResidency8Samples: Boolean = false,
		var sparseResidency16Samples: Boolean = false,
		var sparseResidencyAliased: Boolean = false,
		var variableMultisampleRate: Boolean = false,
		var inheritedQueries: Boolean = false
	) {
		
		internal constructor(features: VkPhysicalDeviceFeatures) : this(
			features.robustBufferAccess(),
			features.fullDrawIndexUint32(),
			features.imageCubeArray(),
			features.independentBlend(),
			features.geometryShader(),
			features.tessellationShader(),
			features.sampleRateShading(),
			features.dualSrcBlend(),
			features.logicOp(),
			features.multiDrawIndirect(),
			features.drawIndirectFirstInstance(),
			features.depthClamp(),
			features.depthBiasClamp(),
			features.fillModeNonSolid(),
			features.depthBounds(),
			features.wideLines(),
			features.largePoints(),
			features.alphaToOne(),
			features.multiViewport(),
			features.samplerAnisotropy(),
			features.textureCompressionETC2(),
			features.textureCompressionASTC_LDR(),
			features.textureCompressionBC(),
			features.occlusionQueryPrecise(),
			features.pipelineStatisticsQuery(),
			features.vertexPipelineStoresAndAtomics(),
			features.fragmentStoresAndAtomics(),
			features.shaderTessellationAndGeometryPointSize(),
			features.shaderImageGatherExtended(),
			features.shaderStorageImageExtendedFormats(),
			features.shaderStorageImageMultisample(),
			features.shaderStorageImageReadWithoutFormat(),
			features.shaderStorageImageWriteWithoutFormat(),
			features.shaderUniformBufferArrayDynamicIndexing(),
			features.shaderSampledImageArrayDynamicIndexing(),
			features.shaderStorageBufferArrayDynamicIndexing(),
			features.shaderStorageImageArrayDynamicIndexing(),
			features.shaderClipDistance(),
			features.shaderCullDistance(),
			features.shaderFloat64(),
			features.shaderInt64(),
			features.shaderInt16(),
			features.shaderResourceResidency(),
			features.shaderResourceMinLod(),
			features.sparseBinding(),
			features.sparseResidencyBuffer(),
			features.sparseResidencyImage2D(),
			features.sparseResidencyImage3D(),
			features.sparseResidency2Samples(),
			features.sparseResidency4Samples(),
			features.sparseResidency8Samples(),
			features.sparseResidency16Samples(),
			features.sparseResidencyAliased(),
			features.variableMultisampleRate(),
			features.inheritedQueries()
		)

		internal fun toVulkan(mem: MemoryStack) =
			VkPhysicalDeviceFeatures.callocStack(mem).apply {
				robustBufferAccess(robustBufferAccess)
				fullDrawIndexUint32(fullDrawIndexUint32)
				imageCubeArray(imageCubeArray)
				independentBlend(independentBlend)
				geometryShader(geometryShader)
				tessellationShader(tessellationShader)
				sampleRateShading(sampleRateShading)
				dualSrcBlend(dualSrcBlend)
				logicOp(logicOp)
				multiDrawIndirect(multiDrawIndirect)
				drawIndirectFirstInstance(drawIndirectFirstInstance)
				depthClamp(depthClamp)
				depthBiasClamp(depthBiasClamp)
				fillModeNonSolid(fillModeNonSolid)
				depthBounds(depthBounds)
				wideLines(wideLines)
				largePoints(largePoints)
				alphaToOne(alphaToOne)
				multiViewport(multiViewport)
				samplerAnisotropy(samplerAnisotropy)
				textureCompressionETC2(textureCompressionETC2)
				textureCompressionASTC_LDR(textureCompressionASTC_LDR)
				textureCompressionBC(textureCompressionBC)
				occlusionQueryPrecise(occlusionQueryPrecise)
				pipelineStatisticsQuery(pipelineStatisticsQuery)
				vertexPipelineStoresAndAtomics(vertexPipelineStoresAndAtomics)
				fragmentStoresAndAtomics(fragmentStoresAndAtomics)
				shaderTessellationAndGeometryPointSize(shaderTessellationAndGeometryPointSize)
				shaderImageGatherExtended(shaderImageGatherExtended)
				shaderStorageImageExtendedFormats(shaderStorageImageExtendedFormats)
				shaderStorageImageMultisample(shaderStorageImageMultisample)
				shaderStorageImageReadWithoutFormat(shaderStorageImageReadWithoutFormat)
				shaderStorageImageWriteWithoutFormat(shaderStorageImageWriteWithoutFormat)
				shaderUniformBufferArrayDynamicIndexing(shaderUniformBufferArrayDynamicIndexing)
				shaderSampledImageArrayDynamicIndexing(shaderSampledImageArrayDynamicIndexing)
				shaderStorageBufferArrayDynamicIndexing(shaderStorageBufferArrayDynamicIndexing)
				shaderStorageImageArrayDynamicIndexing(shaderStorageImageArrayDynamicIndexing)
				shaderClipDistance(shaderClipDistance)
				shaderCullDistance(shaderCullDistance)
				shaderFloat64(shaderFloat64)
				shaderInt64(shaderInt64)
				shaderInt16(shaderInt16)
				shaderResourceResidency(shaderResourceResidency)
				shaderResourceMinLod(shaderResourceMinLod)
				sparseBinding(sparseBinding)
				sparseResidencyBuffer(sparseResidencyBuffer)
				sparseResidencyImage2D(sparseResidencyImage2D)
				sparseResidencyImage3D(sparseResidencyImage3D)
				sparseResidency2Samples(sparseResidency2Samples)
				sparseResidency4Samples(sparseResidency4Samples)
				sparseResidency8Samples(sparseResidency8Samples)
				sparseResidency16Samples(sparseResidency16Samples)
				sparseResidencyAliased(sparseResidencyAliased)
				variableMultisampleRate(variableMultisampleRate)
				inheritedQueries(inheritedQueries)
			}
	}

	val features: Features by lazy {
		memstack { mem ->
			val features = VkPhysicalDeviceFeatures.mallocStack(mem)
			vkGetPhysicalDeviceFeatures(vkDevice, features)
			Features(features)
		}
	}

	data class QueueFamily internal constructor(
		val physicalDevice: PhysicalDevice,
		val index: Int,
		val queueFlags: IntFlags<Flags>,
		val queueCount: Int,
		val timestampValidBits: Int,
		val minImageTransferGranularity: Extent3D
	) {

		enum class Flags(override val value: Int) : IntFlags.Bit {
			Graphics(VK_QUEUE_GRAPHICS_BIT),
			Compute(VK_QUEUE_COMPUTE_BIT),
			Transfer(VK_QUEUE_TRANSFER_BIT),
			Binding(VK_QUEUE_SPARSE_BINDING_BIT)
		}

		override fun toString() = "queue family $index ${queueFlags.toFlagsString()}"

		fun supportsSurface(surface: Surface): Boolean {
			memstack { mem ->
				val pSupported = mem.mallocInt(1)
				vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice.vkDevice, index, surface.id, pSupported)
				return pSupported.get(0) != 0
			}
		}
	}

	val queueFamilies: List<QueueFamily> by lazy {
		memstack { mem ->

			val pCount = mem.mallocInt(1)
			vkGetPhysicalDeviceQueueFamilyProperties(vkDevice, pCount, null)
			val count = pCount.get(0)
			val pQueueFamilies = VkQueueFamilyProperties.mallocStack(count, mem)
			vkGetPhysicalDeviceQueueFamilyProperties(vkDevice, pCount, pQueueFamilies)

			(0 until count)
				.map {
					val qf = pQueueFamilies.get(it)
					QueueFamily(
						this,
						it,
						IntFlags(qf.queueFlags()),
						qf.queueCount(),
						qf.timestampValidBits(),
						qf.minImageTransferGranularity().toExtent3D()
					)
				}
		}
	}

	fun findQueueFamily(flags: IntFlags<QueueFamily.Flags>) =
		queueFamilies
			.find { it.queueFlags.hasAll(flags) }
			?: throw NoSuchElementException("can't find queue family with desired flags")

	fun findQueueFamily(surface: Surface) =
		queueFamilies
			.find {it.supportsSurface(surface) }
			?: throw NoSuchElementException("can't find queue family that supports surface")

	override fun toString() = "${properties.name}: ${properties.uuid}"

	val extensionNames: Set<String> by lazy {
		memstack { mem ->

			val pCount = mem.mallocInt(1)
			vkEnumerateDeviceExtensionProperties(vkDevice, null as String?, pCount, null)
			val count = pCount.get(0)
			val pProperties = VkExtensionProperties.mallocStack(count, mem)
			vkEnumerateDeviceExtensionProperties(vkDevice, null as String?, pCount, pProperties)

			(0 until count)
				.map { pProperties.get().extensionNameString() }
				.toSet()
		}
	}

	fun supportsExtension(name: String): Boolean {
		memstack { mem ->
			return false
		}
	}

	val memoryHeaps: List<MemoryHeap> by lazy {
		memstack { mem ->
			val pMem = VkPhysicalDeviceMemoryProperties.mallocStack(mem)
			vkGetPhysicalDeviceMemoryProperties(vkDevice, pMem)
			(0 until pMem.memoryHeapCount())
				.map { i ->
					val heap = pMem.memoryHeaps(i)
					return@map MemoryHeap(
						i,
						heap.size(),
						IntFlags(heap.flags())
					)
				}
		}
	}

	val memoryTypes: List<MemoryType> by lazy {
		memstack { mem ->
			val pMem = VkPhysicalDeviceMemoryProperties.mallocStack(mem)
			vkGetPhysicalDeviceMemoryProperties(vkDevice, pMem)
			(0 until pMem.memoryTypeCount())
				.map { i ->
					val type = pMem.memoryTypes(i)
					return@map MemoryType(
						i,
						IntFlags(type.propertyFlags()),
						memoryHeaps[type.heapIndex()]
					)
				}
		}
	}

	data class ImageFormatProperties internal constructor(
		val maxExtent: Extent3D,
		val maxMipLevels: Int,
		val maxArrayLayers: Int,
		val sampleCounts: IntFlags<SampleCount>,
		val maxResourceSize: Long
	)

	fun getImageFormatProperties(
		format: Image.Format,
		type: Image.Type,
		usage: IntFlags<Image.Usage>,
		tiling: Image.Tiling,
		flags: IntFlags<Image.Create> = IntFlags(0)
	): ImageFormatProperties {
		memstack { mem ->
			val pProps = VkImageFormatProperties.mallocStack(mem)
			vkGetPhysicalDeviceImageFormatProperties(vkDevice, format.ordinal, type.ordinal, tiling.ordinal, usage.value, flags.value, pProps)
				.orFail("failed to get image format properties")
			return ImageFormatProperties(
				maxExtent = pProps.maxExtent().toExtent3D(),
				maxMipLevels = pProps.maxMipLevels(),
				maxArrayLayers = pProps.maxArrayLayers(),
				sampleCounts = IntFlags(pProps.sampleCounts()),
				maxResourceSize = pProps.maxResourceSize()
			)
		}
	}

	enum class FormatFeatureFlags(override val value: Int) : IntFlags.Bit {
		SampledImage(VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT),
		StorageImage(VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT),
		StorageImageAtomic(VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT),
		UniformTexelBuffer(VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT),
		StorageTexelBuffer(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT),
		StorageTexelBufferAtomic(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT),
		VertexBuffer(VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT),
		ColorAttachment(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT),
		ColorAttachmentBlend(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT),
		DepthStencilAttachment(VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT),
		BlitSrc(VK_FORMAT_FEATURE_BLIT_SRC_BIT),
		BlitDst(VK_FORMAT_FEATURE_BLIT_DST_BIT),
		SampledImageFilterLinear(VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT)
	}

	data class FormatProperties internal constructor(
		val linearTilingFeatures: IntFlags<FormatFeatureFlags>,
		val optimalTilingFeatures: IntFlags<FormatFeatureFlags>,
		val bufferFeatures: IntFlags<FormatFeatureFlags>
	)

	fun getFormatProperties(
		format: Image.Format
	): FormatProperties {
		memstack { mem ->
			val pProps = VkFormatProperties.mallocStack(mem)
			vkGetPhysicalDeviceFormatProperties(vkDevice, format.ordinal, pProps);
			return FormatProperties(
				IntFlags(pProps.linearTilingFeatures()),
				IntFlags(pProps.optimalTilingFeatures()),
				IntFlags(pProps.bufferFeatures())
			)
		}
	}
}

val Vulkan.physicalDevices get(): List<PhysicalDevice> {
	memstack { mem ->

		val pCount = mem.mallocInt(1)
		vkEnumeratePhysicalDevices(instance, pCount, null)
		val count = pCount.get(0)
		val pDevices = mem.mallocPointer(count)
		vkEnumeratePhysicalDevices(instance, pCount, pDevices)

		return (0 until count)
			.map { PhysicalDevice(instance, pDevices.get()) }
	}
}


class Device internal constructor(
	val physicalDevice: PhysicalDevice,
	internal val vkDevice: VkDevice,
	queuePriorities: Map<PhysicalDevice.QueueFamily,List<Float>>
) : AutoCloseable {

	val queues: Map<PhysicalDevice.QueueFamily,List<Queue>> by lazy {
		queuePriorities.mapValues { (family, priorities) ->
			priorities.mapIndexed { i, priority -> Queue(this, family, i) }
		}
	}

	override fun close() {
		_memoryStager?.close()
		vkDestroyDevice(vkDevice, null)
	}

	override fun toString() = "device for ${physicalDevice}"

	fun waitForIdle() {
		vkDeviceWaitIdle(vkDevice)
			.orFail("failed to wait for device")
	}

	private var _memoryStager: MemoryStager? = null
	val memoryStager: MemoryStager get() =
		_memoryStager ?: MemoryStager(this).also { _memoryStager = it }
}

fun PhysicalDevice.device(
	queuePriorities: Map<PhysicalDevice.QueueFamily,List<Float>>,
	features: PhysicalDevice.Features = PhysicalDevice.Features(),
	extensionNames: Set<String> = emptySet(),
	layerNames: Set<String> = emptySet(),
	apiVersion: Version = properties.apiVersion
): Device {
	memstack { mem ->

		// set the queue creation info
		val queueInfos = VkDeviceQueueCreateInfo.callocStack(queuePriorities.size, mem)
		for ((queueFamily, priorities) in queuePriorities) {
			queueInfos.get()
				.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
				.queueFamilyIndex(queueFamily.index)
				.pQueuePriorities(priorities.toBuffer(mem) ?: throw IllegalArgumentException("no queue priorities for $queueFamily"))
		}
		queueInfos.rewind()

		// set device creation info
		val deviceInfo = VkDeviceCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pQueueCreateInfos(queueInfos)
			.pEnabledFeatures(features.toVulkan(mem))
			.ppEnabledExtensionNames(extensionNames.toStringPointerBuffer(mem))
			.ppEnabledLayerNames(layerNames.toStringPointerBuffer(mem))

		// make the device
		val pDevice = mem.mallocPointer(1)
		vkCreateDevice(vkDevice, deviceInfo, null, pDevice)
			.orFail("failed to create device")

		return Device(
			this,
			VkDevice(pDevice.get(0), vkDevice, deviceInfo, apiVersion.value),
			queuePriorities
		)
	}
}

class Queue internal constructor (
	val device: Device,
	val family: PhysicalDevice.QueueFamily,
	val index: Int
) {

	internal val vkQueue: VkQueue = run {
		memstack { mem ->
			val pQueue = mem.mallocPointer(1)
			vkGetDeviceQueue(device.vkDevice, family.index, index, pQueue)
			return@run VkQueue(pQueue.get(0), device.vkDevice)
		}
	}

	// no cleanup needed

	override fun toString() = "queue ${family.index}.$index on $device"

	data class WaitInfo(
		val semaphore: Semaphore,
		val dstStage: IntFlags<PipelineStage>
	)

	fun submit(
		commandBuffer: CommandBuffer,
		waitFor: List<WaitInfo> = emptyList(),
		signalTo: List<Semaphore> = emptyList()
	) {
		memstack { mem ->
			val info = VkSubmitInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
				.pCommandBuffers(commandBuffer.id.toPointerBuffer(mem))
			if (waitFor.isNotEmpty()) {
				info.waitSemaphoreCount(waitFor.size)
				info.pWaitSemaphores(waitFor.map { it.semaphore.id }.toBuffer(mem))
				info.pWaitDstStageMask(waitFor.map { it.dstStage.value }.toBuffer(mem))
			} else {
				info.waitSemaphoreCount(0)
			}
			if (signalTo.isNotEmpty()) {
				info.pSignalSemaphores(signalTo.map { it.id }.toBuffer(mem))
			}
			val fence = VK_NULL_HANDLE // TODO: support fences?
			vkQueueSubmit(vkQueue, info, fence)
				.orFail("failed to submit queue")
		}
	}

	fun present(
		swapchain: Swapchain,
		imageIndex: Int,
		waitFor: Semaphore? = null
	) {
		memstack { mem ->
			val info = VkPresentInfoKHR.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
				.pSwapchains(swapchain.id.toBuffer(mem))
				.swapchainCount(1)
				.pImageIndices(imageIndex.toBuffer(mem))
				.pResults(null)
			if (waitFor != null) {
				info.pWaitSemaphores(waitFor.id.toBuffer(mem))
			}
			vkQueuePresentKHR(vkQueue, info)
				.orFailWhen(VK_ERROR_OUT_OF_DATE_KHR) {
					// convert error to exception, but make sure caller can recognize it and catch it
					throw SwapchainOutOfDateException()
				}
				.orFail("failed to present queue")
		}
	}

	fun waitForIdle() {
		vkQueueWaitIdle(vkQueue)
			.orFail("failed to wait for queue")
	}
}

enum class PipelineStage(override val value: Int): IntFlags.Bit {
	TopOfPipe(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
	DrawIndirect(VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT),
	VertexInput(VK_PIPELINE_STAGE_VERTEX_INPUT_BIT),
	VertexShader(VK_PIPELINE_STAGE_VERTEX_SHADER_BIT),
	TessellationControlShader(VK_PIPELINE_STAGE_TESSELLATION_CONTROL_SHADER_BIT),
	TessellationEvaluationShader(VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT),
	GeometryShader(VK_PIPELINE_STAGE_GEOMETRY_SHADER_BIT),
	FragmentShader(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
	EarlyFragmentTests(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT),
	LateFragmentTests(VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT),
	ColorAttachmentOutput(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
	ComputeShader(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT),
	Transfer(VK_PIPELINE_STAGE_TRANSFER_BIT),
	BottomOfPipe(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT),
	Host(VK_PIPELINE_STAGE_HOST_BIT),
	AllGraphics(VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT),
	AllCommands(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT)
}


class MemoryType(
	internal val index: Int,
	val flags: IntFlags<Flags>,
	val heap: MemoryHeap
) {

	enum class Flags(override val value: Int): IntFlags.Bit {
		DeviceLocal(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
		HostVisible(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),
		HostCoherent(VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
		HostCached(VK_MEMORY_PROPERTY_HOST_CACHED_BIT),
		LazilyAllocated(VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT)
	}

	override fun toString() = "MemoryType[index=$index, flags=${flags.toFlagsString()}, heap=$heap]"
}

class MemoryHeap(
	internal val index: Int,
	val size: Long,
	val flags: IntFlags<Flags>
) {

	enum class Flags(override val value: Int) : IntFlags.Bit {
		DeviceLocal(VK_MEMORY_HEAP_DEVICE_LOCAL_BIT),
		MultiInstance(VK_MEMORY_HEAP_MULTI_INSTANCE_BIT)
	}

	override fun toString() = "MemoryHeap[index=$index, size=$size, flags=${flags.toFlagsString()}]"
}
