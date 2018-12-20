package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toList
import cuchaz.kludge.tools.toUUID
import org.lwjgl.vulkan.*
import java.util.*


class PhysicalDevice internal constructor (internal val instance: VkInstance, internal val id: Long) {

	internal val vkDevice = VkPhysicalDevice(id, instance)

	enum class Type {

		// NOTE: must match order of eg VK10.VK_PHYSICAL_DEVICE_TYPE_OTHER
		Other,
		IntegratedGpu,
		DiscreteGpu,
		VirtualGpu,
		Cpu;

		companion object {
			operator fun get(id: Int) = values()[id]
		}
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
			VK10.vkGetPhysicalDeviceProperties(vkDevice, props)

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

	data class Features internal constructor (
		val robustBufferAccess: Boolean,
		val fullDrawIndexUint32: Boolean,
		val imageCubeArray: Boolean,
		val independentBlend: Boolean,
		val geometryShader: Boolean,
		val tessellationShader: Boolean,
		val sampleRateShading: Boolean,
		val dualSrcBlend: Boolean,
		val logicOp: Boolean,
		val multiDrawIndirect: Boolean,
		val drawIndirectFirstInstance: Boolean,
		val depthClamp: Boolean,
		val depthBiasClamp: Boolean,
		val fillModeNonSolid: Boolean,
		val depthBounds: Boolean,
		val wideLines: Boolean,
		val largePoints: Boolean,
		val alphaToOne: Boolean,
		val multiViewport: Boolean,
		val samplerAnisotropy: Boolean,
		val textureCompressionETC2: Boolean,
		val textureCompressionASTC_LDR: Boolean,
		val textureCompressionBC: Boolean,
		val occlusionQueryPrecise: Boolean,
		val pipelineStatisticsQuery: Boolean,
		val vertexPipelineStoresAndAtomics: Boolean,
		val fragmentStoresAndAtomics: Boolean,
		val shaderTessellationAndGeometryPointSize: Boolean,
		val shaderImageGatherExtended: Boolean,
		val shaderStorageImageExtendedFormats: Boolean,
		val shaderStorageImageMultisample: Boolean,
		val shaderStorageImageReadWithoutFormat: Boolean,
		val shaderStorageImageWriteWithoutFormat: Boolean,
		val shaderUniformBufferArrayDynamicIndexing: Boolean,
		val shaderSampledImageArrayDynamicIndexing: Boolean,
		val shaderStorageBufferArrayDynamicIndexing: Boolean,
		val shaderStorageImageArrayDynamicIndexing: Boolean,
		val shaderClipDistance: Boolean,
		val shaderCullDistance: Boolean,
		val shaderFloat64: Boolean,
		val shaderInt64: Boolean,
		val shaderInt16: Boolean,
		val shaderResourceResidency: Boolean,
		val shaderResourceMinLod: Boolean,
		val sparseBinding: Boolean,
		val sparseResidencyBuffer: Boolean,
		val sparseResidencyImage2D: Boolean,
		val sparseResidencyImage3D: Boolean,
		val sparseResidency2Samples: Boolean,
		val sparseResidency4Samples: Boolean,
		val sparseResidency8Samples: Boolean,
		val sparseResidency16Samples: Boolean,
		val sparseResidencyAliased: Boolean,
		val variableMultisampleRate: Boolean,
		val inheritedQueries: Boolean
	)

	val features: Features by lazy {
		memstack { mem ->
			val features = VkPhysicalDeviceFeatures.mallocStack(mem)
			VK10.vkGetPhysicalDeviceFeatures(vkDevice, features)
			Features(
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
		}
	}

	class QueueFamilyProperties internal constructor(
		val queueFlags: IntFlags,
		val queueCount: Int,
		val timestampValidBits: Int,
		val minImageTransferGranularity: Extent3D
	) {

		enum class Flags(override val value: Int) : IntFlags.Bit {
			Graphics(VK10.VK_QUEUE_GRAPHICS_BIT),
			Compute(VK10.VK_QUEUE_COMPUTE_BIT),
			Transfer(VK10.VK_QUEUE_TRANSFER_BIT),
			Binding(VK10.VK_QUEUE_SPARSE_BINDING_BIT)
		}
	}

	val queueFamilyProperties: List<QueueFamilyProperties> by lazy {
		memstack { mem ->

			val pCount = mem.mallocInt(1)
			VK10.vkGetPhysicalDeviceQueueFamilyProperties(vkDevice, pCount, null)
			val count = pCount.get(0)
			val pQueueFamilies = VkQueueFamilyProperties.mallocStack(count, mem)
			VK10.vkGetPhysicalDeviceQueueFamilyProperties(vkDevice, pCount, pQueueFamilies)

			(0 until count)
				.map {
					val qf = pQueueFamilies.get(it)
					QueueFamilyProperties(
						IntFlags(qf.queueFlags()),
						qf.queueCount(),
						qf.timestampValidBits(),
						qf.minImageTransferGranularity().toExtent3D()
					)
				}
		}
	}

	override fun toString() = "${properties.name}: ${properties.uuid}"
}

val Vulkan.physicalDevices get(): List<PhysicalDevice> {
	memstack { mem ->

		val pCount = mem.mallocInt(1)
		VK10.vkEnumeratePhysicalDevices(instance, pCount, null)
		val count = pCount.get(0)
		val pDevices = mem.mallocPointer(count)
		VK10.vkEnumeratePhysicalDevices(instance, pCount, pDevices)

		return (0 until count)
			.map { PhysicalDevice(instance, pDevices.get()) }
	}
}
