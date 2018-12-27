/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugUtils.*


class DebugMessenger internal constructor(
	val vulkan: Vulkan,
	internal val id: Long
) : AutoCloseable {

	enum class Severity(override val value: Int) : IntFlags.Bit {
		Error(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT),
		Warning(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT),
		Info(VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT),
		Verbose(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT)
	}

	enum class Type(override val value: Int) : IntFlags.Bit {
		General(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT),
		Validation(VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT),
		Performance(VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
	}

	override fun close() {
		vkDestroyDebugUtilsMessengerEXT(vulkan.instance, id, null)
	}
}

fun Vulkan.debugMessenger(
	severities: IntFlags<DebugMessenger.Severity> = IntFlags.of(*DebugMessenger.Severity.values()),
	types: IntFlags<DebugMessenger.Type> = IntFlags.of(*DebugMessenger.Type.values()),
	block: (IntFlags<DebugMessenger.Severity>, IntFlags<DebugMessenger.Type>, String) -> Unit
): DebugMessenger {
	memstack { mem ->
		val infoCreate = VkDebugUtilsMessengerCreateInfoEXT.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
			.messageSeverity(severities.value)
			.messageType(types.value)
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
		vkCreateDebugUtilsMessengerEXT(instance, infoCreate, null, pCallback)
			.orFail("failed to create debug util")
		return DebugMessenger(this, pCallback.get(0))
	}
}

fun Vulkan.debugSend(severity: DebugMessenger.Severity, type: DebugMessenger.Type, msg: String) {
	memstack { mem ->

		// create a dummy object
		val pObjects = VkDebugUtilsObjectNameInfoEXT.callocStack(1, mem)

		val pCallbackData = VkDebugUtilsMessengerCallbackDataEXT.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CALLBACK_DATA_EXT)
			.pMessage(mem.UTF8(msg))
			.pObjects(pObjects)

		vkSubmitDebugUtilsMessageEXT(
			instance,
			severity.value,
			type.value,
			pCallbackData
		)
	}
}

fun Vulkan.debugError(msg: String, type: DebugMessenger.Type = DebugMessenger.Type.General) =
	debugSend(DebugMessenger.Severity.Error, type, msg)

fun Vulkan.debugWarn(msg: String, type: DebugMessenger.Type = DebugMessenger.Type.General) =
	debugSend(DebugMessenger.Severity.Warning, type, msg)

fun Vulkan.debugInfo(msg: String, type: DebugMessenger.Type = DebugMessenger.Type.General) =
	debugSend(DebugMessenger.Severity.Info, type, msg)
