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
import org.lwjgl.vulkan.VK10.*
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
		val robustBufferAccess: Boolean = false,
		val fullDrawIndexUint32: Boolean = false,
		val imageCubeArray: Boolean = false,
		val independentBlend: Boolean = false,
		val geometryShader: Boolean = false,
		val tessellationShader: Boolean = false,
		val sampleRateShading: Boolean = false,
		val dualSrcBlend: Boolean = false,
		val logicOp: Boolean = false,
		val multiDrawIndirect: Boolean = false,
		val drawIndirectFirstInstance: Boolean = false,
		val depthClamp: Boolean = false,
		val depthBiasClamp: Boolean = false,
		val fillModeNonSolid: Boolean = false,
		val depthBounds: Boolean = false,
		val wideLines: Boolean = false,
		val largePoints: Boolean = false,
		val alphaToOne: Boolean = false,
		val multiViewport: Boolean = false,
		val samplerAnisotropy: Boolean = false,
		val textureCompressionETC2: Boolean = false,
		val textureCompressionASTC_LDR: Boolean = false,
		val textureCompressionBC: Boolean = false,
		val occlusionQueryPrecise: Boolean = false,
		val pipelineStatisticsQuery: Boolean = false,
		val vertexPipelineStoresAndAtomics: Boolean = false,
		val fragmentStoresAndAtomics: Boolean = false,
		val shaderTessellationAndGeometryPointSize: Boolean = false,
		val shaderImageGatherExtended: Boolean = false,
		val shaderStorageImageExtendedFormats: Boolean = false,
		val shaderStorageImageMultisample: Boolean = false,
		val shaderStorageImageReadWithoutFormat: Boolean = false,
		val shaderStorageImageWriteWithoutFormat: Boolean = false,
		val shaderUniformBufferArrayDynamicIndexing: Boolean = false,
		val shaderSampledImageArrayDynamicIndexing: Boolean = false,
		val shaderStorageBufferArrayDynamicIndexing: Boolean = false,
		val shaderStorageImageArrayDynamicIndexing: Boolean = false,
		val shaderClipDistance: Boolean = false,
		val shaderCullDistance: Boolean = false,
		val shaderFloat64: Boolean = false,
		val shaderInt64: Boolean = false,
		val shaderInt16: Boolean = false,
		val shaderResourceResidency: Boolean = false,
		val shaderResourceMinLod: Boolean = false,
		val sparseBinding: Boolean = false,
		val sparseResidencyBuffer: Boolean = false,
		val sparseResidencyImage2D: Boolean = false,
		val sparseResidencyImage3D: Boolean = false,
		val sparseResidency2Samples: Boolean = false,
		val sparseResidency4Samples: Boolean = false,
		val sparseResidency8Samples: Boolean = false,
		val sparseResidency16Samples: Boolean = false,
		val sparseResidencyAliased: Boolean = false,
		val variableMultisampleRate: Boolean = false,
		val inheritedQueries: Boolean = false
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
		val queueFlags: IntFlags,
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

		override fun toString() = "queue family $index (${queueFlags.toString(Flags.values())})"

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

	fun findQueueFamily(flags: IntFlags) =
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
		vkDestroyDevice(vkDevice, null)
	}

	override fun toString() = "device for ${physicalDevice}"
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
				.pQueuePriorities(priorities.toBuffer(mem))
		}
		queueInfos.rewind()

		// set device creation info
		val deviceInfo = VkDeviceCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
			.pQueueCreateInfos(queueInfos)
			.pEnabledFeatures(features.toVulkan(mem))
			.ppEnabledExtensionNames(extensionNames.toPointerBuffer(mem))
			.ppEnabledLayerNames(layerNames.toPointerBuffer(mem))

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
}

