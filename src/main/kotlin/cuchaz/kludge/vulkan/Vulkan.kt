/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.Kludge
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toPointerBuffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class Vulkan(
	val applicationName: String = "",
	val extensionNames: Set<String> = emptySet(),
	val layerNames: Set<String> = emptySet()
) : AutoCloseable {

	companion object {

		private val errorMessages = mapOf(
			VK_SUCCESS to "VK_Sval UCCESS: Command successfully completed",
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
			VK_ERROR_FRAGMENTED_POOL to "VK_ERROR_FRAGMENTED_POOL: A pool allocation has failed due to fragmentation of the pool’s memory"
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
					.orFail("Failed to query supported extensions")
				val count = pCount.get(0)
				val pProps = VkExtensionProperties.mallocStack(count)
				vkEnumerateInstanceExtensionProperties(null as String?, pCount, pProps)
				(0 until count)
					.map { pProps.get().extensionNameString() }
					.toSet()
			}
		}

		const val DebugExtension = EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME

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
				.ppEnabledExtensionNames(extensionNames.toPointerBuffer(mem))
				.ppEnabledLayerNames(layerNames.toPointerBuffer(mem))

			val pInstance = mem.mallocPointer(1)
			vkCreateInstance(infoCreate, null, pInstance)
				.orFail("failed to create Vulkan instance")
			return@run VkInstance(pInstance.get(0), infoCreate)
		}
	}

	override fun close() {

		// cleanup the instance last
		vkDestroyInstance(instance, null)
	}
}
