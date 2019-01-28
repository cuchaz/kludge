package cuchaz.kludge.imgui

import com.sun.jna.Native
import com.sun.jna.Structure
import cuchaz.kludge.vulkan.*
import cuchaz.kludge.window.Window


object Imgui : AutoCloseable {

	internal object native {

		external fun igGetVersion(): String

		external fun igCreateContext(fontAtlasId: Long): Long
		external fun igDestroyContext(id: Long)

		external fun igStyleColorsDark(styleId: Long)
		external fun igStyleColorsClassic(styleId: Long)
		external fun igStyleColorsLight(styleId: Long)

		external fun ImFontAtlas_ImFontAtlas(): Long
		external fun ImFontAtlas_destroy(id: Long)

		external fun ImGuiStyle_ImGuiStyle(): Long
		external fun ImGuiStyle_destroy(id: Long)

		external fun ImGui_ImplGlfw_InitForVulkan(windowId: Long, installCallbacks: Boolean)
		external fun ImGui_ImplGlfw_Shutdown()

		@Structure.FieldOrder(
			"instanceId", "physicalDeviceId", "deviceId", "queueFamilyIndex", "queueId",
			"pipelineCacheId", "descriptorPoolId", "allocatorId", "errFn"
		)
		class InitInfo(
			@JvmField var instanceId: Long = 0,
			@JvmField var physicalDeviceId: Long = 0,
			@JvmField var deviceId: Long = 0,
			@JvmField var queueFamilyIndex: Int = 0,
			@JvmField var queueId: Long = 0,
			@JvmField var pipelineCacheId: Long = 0,
			@JvmField var descriptorPoolId: Long = 0,
			@JvmField var allocatorId: Long = 0,
			@JvmField var errFn: Long = 0
		) : Structure()

		external fun ImGui_ImplVulkan_Init(initInfo: InitInfo, renderPassId: Long)
		external fun ImGui_ImplVulkan_Shutdown()
	}

	fun load() {
		Native.register(native.javaClass, "kludge-imgui")
	}

	val version: String get() = native.igGetVersion()

	enum class StyleColors {
		Dark,
		Classic,
		Light
	}

	var styleColors: StyleColors = StyleColors.Dark
		set(value) {
			field = value
			when (value) {
				StyleColors.Dark -> native.igStyleColorsDark(0)
				StyleColors.Classic -> native.igStyleColorsClassic(0)
				StyleColors.Light -> native.igStyleColorsLight(0)
			}
		}


	fun init(
		window: Window,
		vulkan: Vulkan,
		physicalDevice: PhysicalDevice,
		device: Device,
		queueFamily: PhysicalDevice.QueueFamily,
		queue: Queue,
		descriptorPool: DescriptorPool,
		renderPass: RenderPass,
		installCallbacks: Boolean = false // TODO: what should the default be?
	) {
		native.ImGui_ImplGlfw_InitForVulkan(window.id, installCallbacks)
		native.ImGui_ImplVulkan_Init(
			native.InitInfo(
				instanceId = vulkan.instance.address(),
				physicalDeviceId = physicalDevice.id,
				deviceId = device.vkDevice.address(),
				queueFamilyIndex = queueFamily.index,
				queueId =  queue.vkQueue.address(),
				descriptorPoolId = descriptorPool.id
			),
			renderPass.id
		)
	}

	override fun close() {
		native.ImGui_ImplVulkan_Shutdown()
		native.ImGui_ImplGlfw_Shutdown()
	}
}
