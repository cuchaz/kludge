/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.imgui

import com.sun.jna.Native
import com.sun.jna.Structure
import cuchaz.kludge.tools.AutoCloser
import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.vulkan.*
import cuchaz.kludge.window.Window
import org.lwjgl.system.MemoryUtil


object Imgui : AutoCloseable {

	internal object native {

		external fun igGetVersion(): String

		external fun igCreateContext(fontAtlasId: Long): Long
		external fun igDestroyContext(id: Long)
		external fun igGetCurrentContext(): Long

		external fun igStyleColorsDark(styleId: Long)
		external fun igStyleColorsClassic(styleId: Long)
		external fun igStyleColorsLight(styleId: Long)

		external fun igNewFrame()
		external fun igRender()
		external fun igGetDrawData(): Long

		external fun igBegin(name: String, open: Long, flags: Int): Boolean
		external fun igEnd()
		external fun igSetNextWindowSize(size: Vec2.ByVal, cond: Int)

		external fun igShowDemoWindow(open: Long)
		external fun igShowAboutWindow(open: Long)
		external fun igShowMetricsWindow(open: Long)
		external fun igShowUserGuide()

		external fun igText(fmt: String)
		external fun igCheckbox(label: String, v: Long): Boolean
		external fun igButton(label: String, size: Vec2.ByVal): Boolean
		external fun igSmallButton(label: String): Boolean

		external fun igSameLine(pos: Float, spacing: Float)

		external fun igGetIO(): Long

		external fun ImFontAtlas_ImFontAtlas(): Long
		external fun ImFontAtlas_destroy(id: Long)

		external fun ImGuiStyle_ImGuiStyle(): Long
		external fun ImGuiStyle_destroy(id: Long)

		external fun ImGui_ImplGlfw_InitForVulkan(windowId: Long, installCallbacks: Boolean)
		external fun ImGui_ImplGlfw_Shutdown()
		external fun ImGui_ImplGlfw_NewFrame()

		external fun ImGui_ImplVulkan_Init(initInfo: InitInfo, renderPassId: Long)
		external fun ImGui_ImplVulkan_Shutdown()
		external fun ImGui_ImplVulkan_CreateFontsTexture(commandBufferId: Long)
		external fun ImGui_ImplVulkan_NewFrame()
		external fun ImGui_ImplVulkan_RenderDrawData(drawData: Long, commandBufId: Long)

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

		@Structure.FieldOrder("x", "y")
		sealed class Vec2(
			@JvmField var x: Float,
			@JvmField var y: Float
		) : Structure() {

			class ByVal(x: Float = 0f, y: Float = 0f) : Vec2(x, y), Structure.ByValue
			class ByRef(x: Float = 0f, y: Float = 0f) : Vec2(x, y), Structure.ByReference

			//class ByValue(x: Float, y: Float) : Vec2(x, y), Structure.ByValue
			//class ByRef(x: Float, y: Float) : Vec2(x, y), Structure.ByReference
		}
	}

	fun load() = apply {
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

	private var stateOrNull: State? = null
	private val state: State get() = stateOrNull ?: throw NoSuchElementException("call init() first")

	private class State(
		val queue: Queue
	) : AutoCloseable {

		val autoCloser = AutoCloser()
		fun <T:AutoCloseable> T.autoClose(): T = autoCloser.add(this)
		override fun close() = autoCloser.close()

		val device = queue.device

		val commandPool = device.commandPool(queue.family).autoClose()
	}

	fun init(
		window: Window,
		queue: Queue,
		descriptorPool: DescriptorPool,
		renderPass: RenderPass,
		installCallbacks: Boolean = false // TODO: what should the default be?
	) {

		// init native side
		native.ImGui_ImplGlfw_InitForVulkan(window.id, installCallbacks)
		native.ImGui_ImplVulkan_Init(
			native.InitInfo(
				instanceId = queue.device.physicalDevice.instance.address(),
				physicalDeviceId = queue.device.physicalDevice.id,
				deviceId = queue.device.vkDevice.address(),
				queueFamilyIndex = queue.family.index,
				queueId =  queue.vkQueue.address(),
				descriptorPoolId = descriptorPool.id
			),
			renderPass.id
		)

		// init kotlin side
		stateOrNull = State(queue)
	}

	override fun close() {
		stateOrNull?.close()
		stateOrNull = null
		native.ImGui_ImplVulkan_Shutdown()
		native.ImGui_ImplGlfw_Shutdown()
	}

	// TODO: allow loading additional fonts?
	fun initFonts() = state.run {
		queue.submit(commandPool.buffer().apply {
			begin(IntFlags.of(CommandBuffer.Usage.OneTimeSubmit))
			native.ImGui_ImplVulkan_CreateFontsTexture(id)
			end()
		})
		device.waitForIdle()
	}

	private val commands = Commands()

	fun frame(block: Commands.() -> Unit) {
		native.ImGui_ImplVulkan_NewFrame()
		native.ImGui_ImplGlfw_NewFrame()
		native.igNewFrame()
		commands.block()
		native.igRender()
	}

	fun draw(buf: CommandBuffer) {
		val drawData = native.igGetDrawData()
		if (drawData != 0L) {
			native.ImGui_ImplVulkan_RenderDrawData(drawData, buf.id)
		}
	}

	object io {

		fun getFloat(offset: Int) = MemoryUtil.memGetFloat(native.igGetIO() + offset)

		/* TODO: make accessors for all the fields
			size	offset	descriptor
			4	0	    ImGuiConfigFlags ConfigFlags;
			4	4	    ImGuiBackendFlags BackendFlags;
			8	8	    ImVec2 DisplaySize;
			4	16	    float DeltaTime;
			4	20	    float IniSavingRate;
			8	24	    const char* IniFilename;
			8	32	    const char* LogFilename;
			4	40	    float MouseDoubleClickTime;
			4	44	    float MouseDoubleClickMaxDist;
			4	48	    float MouseDragThreshold;
			84	52	    int KeyMap[ImGuiKey_COUNT];
			4	136	    float KeyRepeatDelay;
			4	140	    float KeyRepeatRate;
			8	144	    void* UserData;
			8	152	    ImFontAtlas*Fonts;
			4	160	    float FontGlobalScale;
			4	164	    bool FontAllowUserScaling;
			8	168	    ImFont* FontDefault;
			8	176	    ImVec2 DisplayFramebufferScale;
			8	184	    ImVec2 DisplayVisibleMin;
			8	192	    ImVec2 DisplayVisibleMax;
			1	200	    bool MouseDrawCursor;
			1	201	    bool ConfigMacOSXBehaviors;
			1	202	    bool ConfigInputTextCursorBlink;
			1	203	    bool ConfigWindowsResizeFromEdges;
			4	204	    bool ConfigWindowsMoveFromTitleBarOnly;
			8	208	    const char* BackendPlatformName;
			8	216	    const char* BackendRendererName;
			8	224	    void* BackendPlatformUserData;
			8	232	    void* BackendRendererUserData;
			8	240	    void* BackendLanguageUserData;
			8	248	    const char* (*GetClipboardTextFn)(void* user_data);
			8	256	    void (*SetClipboardTextFn)(void* user_data, const char* text);
			8	264	    void* ClipboardUserData;
			8	272	    void (*ImeSetInputScreenPosFn)(int x, int y);
			8	280	    void* ImeWindowHandle;
			8	288	    void* RenderDrawListsFnUnused;
			8	296	    ImVec2 MousePos;
			8	304	    bool MouseDown[5];
			4	312	    float MouseWheel;
			4	316	    float MouseWheelH;
			1	320	    bool KeyCtrl;
			1	321	    bool KeyShift;
			1	322	    bool KeyAlt;
			1	323	    bool KeySuper;
			512	324	    bool KeysDown[512];
			84	836	    float NavInputs[ImGuiNavInput_COUNT];
			1	920	    bool WantCaptureMouse;
			1	921	    bool WantCaptureKeyboard;
			1	922	    bool WantTextInput;
			1	923	    bool WantSetMousePos;
			1	924	    bool WantSaveIniSettings;
			1	925	    bool NavActive;
			2	926	    bool NavVisible;
			4	928	    float Framerate;
			4	932	    int MetricsRenderVertices;
			4	936	    int MetricsRenderIndices;
			4	940	    int MetricsRenderWindows;
			4	944	    int MetricsActiveWindows;
			4	948	    int MetricsActiveAllocations;
			8	952	    ImVec2 MouseDelta;
			8	960	    ImVec2 MousePosPrev;
			8	968	    ImVec2 MouseClickedPos[5];
			8	976	    double MouseClickedTime[5];
			1	984	    bool MouseClicked[5];
			1	985	    bool MouseDoubleClicked[5];
			1	986	    bool MouseReleased[5];
			1	987	    bool MouseDownOwned[5];
			4	988	    float MouseDownDuration[5];
			4	992	    float MouseDownDurationPrev[5];
			8	996	    ImVec2 MouseDragMaxDistanceAbs[5];
			4	1004    float MouseDragMaxDistanceSqr[5];
			4	1008    float KeysDownDuration[512];
			4	1012    float KeysDownDurationPrev[512];
			4	1016    float NavInputsDownDuration[ImGuiNavInput_COUNT];
			4	1020    float NavInputsDownDurationPrev[ImGuiNavInput_COUNT];
			?	1024    ImVector_ImWchar InputQueueCharacters;
		 */
		object displaySize {
			val width: Float get() = getFloat(8)
			val height: Float get() = getFloat(8+4)
		}
		val deltaTime: Float get() = getFloat(16)
		val frameRate: Float get() = getFloat(928)
	}
}
