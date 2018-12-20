/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*


interface DebugMessager : AutoCloseable {

	enum class Severity(override val value: Int) : IntFlags.Bit {
		Error(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT),
		Warning(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT),
		Info(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT),
		Verbose(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT)
	}

	enum class Type(override val value: Int) : IntFlags.Bit {
		General(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT),
		Validation(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT),
		Performance(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
	}
}

fun Vulkan.debugMessager(
	desiredSeverities: IntFlags = IntFlags.of(*DebugMessager.Severity.values()),
	desiredTypes: IntFlags = IntFlags.of(*DebugMessager.Type.values()),
	block: (IntFlags, IntFlags, String) -> Unit
) = object : DebugMessager {

	private val debugId: Long = run {
		memstack { mem ->
			val infoCreate = VkDebugUtilsMessengerCreateInfoEXT.callocStack(mem)
				.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
				.messageSeverity(desiredSeverities.value)
				.messageType(desiredTypes.value)
				.pfnUserCallback { messageSeverity, messageType, pCallbackData, pUserData ->

					// unpack the callback data
					val callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)

					/* TODO: is anything useful ever sent in the objects?
					for (i in 0 until callbackData.objectCount()) {
						val obj = callbackData.pObjects().get()
						println("object: ${obj.pObjectNameString()}  type: ${obj.objectType()}  handle: ${obj.objectHandle()}")
					}
					*/

					// route the callback to the block arg
					block(IntFlags(messageSeverity), IntFlags(messageType), callbackData.pMessageString())

					return@pfnUserCallback false.toVulkan()
				}
			val pCallback = mem.mallocLong(1)
			EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(instance, infoCreate, null, pCallback)
				.orFail("Failed to create debug util")
			return@run pCallback.get(0)
		}
	}

	override fun close() {
		EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugId, null)
	}
}

fun Vulkan.debugSend(severities: IntFlags, types: IntFlags, msg: String) {
	memstack { mem ->

		// create a dummy object
		val pObjects = VkDebugUtilsObjectNameInfoEXT.callocStack(1, mem)

		val pCallbackData = VkDebugUtilsMessengerCallbackDataEXT.callocStack(mem)
			.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CALLBACK_DATA_EXT)
			.pMessage(MemoryUtil.memUTF8(msg))
			.pObjects(pObjects)

		EXTDebugUtils.vkSubmitDebugUtilsMessageEXT(
			instance,
			severities.value,
			types.value,
			pCallbackData
		)
	}
}

fun Vulkan.debugError(msg: String) =
	debugSend(
		IntFlags.of(DebugMessager.Severity.Error),
		IntFlags.of(DebugMessager.Type.General),
		msg
	)

fun Vulkan.debugWarn(msg: String) =
	debugSend(
		IntFlags.of(DebugMessager.Severity.Warning),
		IntFlags.of(DebugMessager.Type.General),
		msg
	)

fun Vulkan.debugInfo(msg: String) =
	debugSend(
		IntFlags.of(DebugMessager.Severity.Info),
		IntFlags.of(DebugMessager.Type.General),
		msg
	)
