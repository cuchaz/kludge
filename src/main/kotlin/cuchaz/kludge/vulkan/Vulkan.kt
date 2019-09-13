/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.Kludge
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toStringPointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.EXTDescriptorIndexing.VK_ERROR_FRAGMENTATION_EXT
import org.lwjgl.vulkan.EXTGlobalPriority.VK_ERROR_NOT_PERMITTED_EXT
import org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR
import org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR
import org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
import org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR
import org.lwjgl.vulkan.NVGLSLShader.VK_ERROR_INVALID_SHADER_NV
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.VK_ERROR_INVALID_EXTERNAL_HANDLE
import org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY


class Vulkan(
	val applicationName: String = "",
	val extensionNames: Set<String> = emptySet(),
	val layerNames: Set<String> = emptySet()
) : AutoCloseable {

	companion object {

		private val errorMessages = mapOf(
			VK_SUCCESS to "VK_SUCCESS: Command successfully completed",
			VK_NOT_READY to "VK_NOT_READY: A fence or query has not yet completed",
			VK_TIMEOUT to "VK_TIMEOUT: A wait operation has not completed in the specified time",
			VK_EVENT_SET to "VK_EVENT_SET: An event is signaled",
			VK_EVENT_RESET to "VK_EVENT_RESET: An event is unsignaled",
			VK_INCOMPLETE to "VK_IMCOMPLETE: A return array was too small for the result",
			VK_ERROR_OUT_OF_HOST_MEMORY to "VK_ERROR_OUT_OF_HOST_MEMORY: A host memory allocation has failed",
			VK_ERROR_OUT_OF_DEVICE_MEMORY to "VK_ERROR_OUT_OF_DEVICE_MEMORY: A device memory allocation has failed",
			VK_ERROR_INITIALIZATION_FAILED to "VK_ERROR_INITIALIZATION_FAILED: Initialization of an object could not be completed for implementation-specific reasons",
			VK_ERROR_DEVICE_LOST to "VK_ERROR_DEVICE_LOST: The logical or physical device has been lost",
			VK_ERROR_MEMORY_MAP_FAILED to "VK_ERROR_MEMORY_MAP_FAILED: Mapping of a memory object has failed",
			VK_ERROR_LAYER_NOT_PRESENT to "VK_ERROR_LAYER_NOT_PRESENT: A requested layer is not present or could not be loaded",
			VK_ERROR_EXTENSION_NOT_PRESENT to "VK_ERROR_EXTENSION_NOT_PRESENT: A requested extension is not supported",
			VK_ERROR_FEATURE_NOT_PRESENT to "VK_ERROR_FEATURE_NOT_PRESENT: A requested feature is not supported",
			VK_ERROR_INCOMPATIBLE_DRIVER to "VK_ERROR_INCOMPATIBLE_DRIVER: The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons",
			VK_ERROR_TOO_MANY_OBJECTS to "VK_ERROR_TOO_MANY_OBJECTS: Too many objects of the type have already been created",
			VK_ERROR_FORMAT_NOT_SUPPORTED to "VK_ERROR_FORMAT_NOT_SUPPORTED: A requested format is not supported on this device",
			VK_ERROR_FRAGMENTED_POOL to "VK_ERROR_FRAGMENTED_POOL: A pool allocation has failed due to fragmentation of the pool’s memory",

			VK_ERROR_OUT_OF_POOL_MEMORY to "VK_ERROR_OUT_OF_POOL_MEMORY: A pool memory allocation has failed.",
			VK_ERROR_INVALID_EXTERNAL_HANDLE to "VK_ERROR_INVALID_EXTERNAL_HANDLE: An external handle is not a valid handle of the specified type.",
			VK_ERROR_SURFACE_LOST_KHR to "VK_ERROR_SURFACE_LOST_KHR: A surface is no longer available.",
			VK_ERROR_NATIVE_WINDOW_IN_USE_KHR to "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR: The requested window is already in use by Vulkan or another API in a manner which prevents it from being used again.",
			VK_ERROR_OUT_OF_DATE_KHR to "VK_ERROR_OUT_OF_DATE_KHR: A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the swapchain will fail.",
			VK_ERROR_INCOMPATIBLE_DISPLAY_KHR to "VK_ERROR_INCOMPATIBLE_DISPLAY_KHR: The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an image.",
			VK_ERROR_VALIDATION_FAILED_EXT to "VK_ERROR_VALIDATION_FAILED_EXT: A layer callback has aborted this function call.",
			VK_ERROR_INVALID_SHADER_NV to "VK_ERROR_INVALID_SHADER_NV: One or more shaders failed to compile or link. More details are reported back to the application via https://www.khronos.org/registry/vulkan/specs/1.1-extensions/html/vkspec.html#VK_EXT_debug_report if enabled.",
			/* VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT */ -1000158000 to "VK_ERROR_INVALID_DRM_FORMAT_MODIFIER_PLANE_LAYOUT_EXT",
			VK_ERROR_FRAGMENTATION_EXT to "VK_ERROR_FRAGMENTATION_EXT: A descriptor pool creation has failed due to fragmentation.",
			VK_ERROR_NOT_PERMITTED_EXT to "VK_ERROR_NOT_PERMITTED_EXT",
			/* VK_ERROR_INVALID_DEVICE_ADDRESS_EXT */ -1000244000 to "VK_ERROR_INVALID_DEVICE_ADDRESS_EXT: A buffer creation failed because the requested address is not available.",
			/* VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT */ -1000255000 to "VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT: An operation on a swapchain created with VK_FULL_SCREEN_EXCLUSIVE_APPLICATION_CONTROLLED_EXT failed as it did not have exlusive full-screen access. This may occur due to implementation-dependent reasons, outside of the application’s control."
		)

		interface Errors {
			operator fun get(err: Int): String
		}
		val errors = object : Errors {
			override operator fun get(err: Int) = errorMessages[err] ?: "Unknown error: $err"
		}

		val supportedExtensions: Set<String> by lazy {
			memstack { mem ->
				val pCount = mem.mallocInt(1)
				vkEnumerateInstanceExtensionProperties(null as String?, pCount, null)
					.orFail("failed to query supported extensions")
				val count = pCount.get(0)
				val pProps = VkExtensionProperties.mallocStack(count)
				vkEnumerateInstanceExtensionProperties(null as String?, pCount, pProps)
				(0 until count)
					.map { pProps.get().extensionNameString() }
					.toSet()
			}
		}

		const val DebugExtension = VK_EXT_DEBUG_UTILS_EXTENSION_NAME

		val supportedLayers: Set<String> by lazy {
			memstack { mem ->
				val pCount = mem.mallocInt(1)
				vkEnumerateInstanceLayerProperties(pCount, null)
				val count = pCount.get(0)
				val pLayers = VkLayerProperties.mallocStack(count)
				vkEnumerateInstanceLayerProperties(pCount, pLayers)
				(0 until count)
					.map { pLayers.get().layerNameString() }
					.toSet()
			}
		}

		const val StandardValidationLayer = "VK_LAYER_LUNARG_standard_validation"
	}

	internal val instance: VkInstance = run {
		memstack { mem ->

			val infoApp = VkApplicationInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
				.pEngineName(MemoryUtil.memUTF8(Kludge.name))
				.pApplicationName(MemoryUtil.memUTF8(applicationName))
				.apiVersion(VK_API_VERSION_1_0) // TODO: make version a parameter?

			val infoCreate = VkInstanceCreateInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
				.pApplicationInfo(infoApp)
				.ppEnabledExtensionNames(extensionNames.toStringPointerBuffer(mem))
				.ppEnabledLayerNames(layerNames.toStringPointerBuffer(mem))

			val pInstance = mem.mallocPointer(1)
			vkCreateInstance(infoCreate, null, pInstance)
				.orFailMsg(VK_ERROR_EXTENSION_NOT_PRESENT) {
					"Requested extensions: $extensionNames" +
					"\nUnsuppored extensions: ${extensionNames.filter { it !in supportedExtensions }}"

				}
				.orFailMsg(VK_ERROR_LAYER_NOT_PRESENT) {
					"Requested layers: $layerNames" +
					"\nUnsupported layers: ${layerNames.filter { it !in supportedLayers }}}"
				}
				.orFail("failed to create Vulkan instance")
			return@run VkInstance(pInstance.get(0), infoCreate)
		}
	}

	override fun close() {

		// cleanup the instance last
		vkDestroyInstance(instance, null)
	}
}
