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
import org.joml.Vector2f
import org.lwjgl.system.MemoryUtil


object Imgui : AutoCloseable {

	internal object native {

		external fun igCreateContext(fontAtlasId: Long): Long
		external fun igDestroyContext(id: Long)
		external fun igGetCurrentContext(): Long

		external fun igGetIO(): Long
		external fun igNewFrame()
		external fun igRender()
		external fun igGetDrawData(): Long

		external fun igShowDemoWindow(open: Long)
		external fun igShowAboutWindow(open: Long)
		external fun igShowMetricsWindow(open: Long)
		external fun igShowUserGuide()
		external fun igGetVersion(): String

		external fun igStyleColorsDark(styleId: Long)
		external fun igStyleColorsClassic(styleId: Long)
		external fun igStyleColorsLight(styleId: Long)

		external fun igBegin(name: String, open: Long, flags: Int): Boolean
		external fun igEnd()

		external fun igIsWindowAppearing(): Boolean
		external fun igIsWindowCollapsed(): Boolean
		external fun igIsWindowFocused(flags: Int): Boolean
		external fun igIsWindowHovered(flags: Int): Boolean
		// TODO: igGetWindowDrawList?
		external fun igGetWindowPos(): Vec2.ByVal
		external fun igGetWindowSize(): Vec2.ByVal
		external fun igGetWindowWidth(): Float
		external fun igGetWindowHeight(): Float
		external fun igGetContentRegionMax(): Vec2.ByVal
		external fun igGetContentRegionAvail(): Vec2.ByVal
		external fun igGetContentRegionAvailWidth(): Float
		external fun igGetWindowContentRegionMin(): Vec2.ByVal
		external fun igGetWindowContentRegionMax(): Vec2.ByVal
		external fun igGetWindowContentRegionWidth(): Float

		external fun igSetNextWindowPos(pos: Vec2.ByVal, cond: Int, pivot: Vec2.ByVal)
		external fun igSetNextWindowSize(size: Vec2.ByVal, cond: Int)
		// TODO: igSetNextWindowSizeConstraints?
		external fun igSetNextWindowContentSize(size: Vec2.ByVal)
		external fun igSetNextWindowCollapsed(collapsed: Boolean, cond: Int)
		external fun igSetNextWindowFocus()
		external fun igSetNextWindowBgAlpha(alpha: Float)
		external fun igSetWindowFontScale(scale: Float)
		external fun igSetWindowPosStr(name: String, pos: Vec2.ByVal, cond: Int)
		external fun igSetWindowSizeStr(name: String, size: Vec2.ByVal, cond: Int)
		external fun igSetWindowCollapsedStr(name: String, collapsed: Boolean, cond: Int)
		external fun igSetWindowFocusStr(name: String)

		external fun igSeparator()
		external fun igSameLine(pos: Float, spacing: Float)
		external fun igNewLine()
		external fun igSpacing()
		external fun igDummy(size: Vec2.ByVal)
		external fun igIndent(indent_w: Float)
		external fun igUnindent(indent_w: Float)
		external fun igBeginGroup()
		external fun igEndGroup()
		external fun igGetCursorPos(): Vec2.ByVal
		external fun igGetCursorPosX(): Float
		external fun igGetCursorPosY(): Float
		external fun igSetCursorPos(local_pos: Vec2.ByVal)
		external fun igSetCursorPosX(local_x: Float)
		external fun igSetCursorPosY(local_y: Float)
		external fun igGetCursorStartPos(): Vec2.ByVal
		external fun igGetCursorScreenPos(): Vec2.ByVal
		external fun igSetCursorScreenPos(pos: Vec2.ByVal)
		external fun igAlignTextToFramePadding()
		external fun igGetTextLineHeight(): Float
		external fun igGetTextLineHeightWithSpacing(): Float
		external fun igGetFrameHeight(): Float
		external fun igGetFrameHeightWithSpacing(): Float

		external fun igText(fmt: String)

		external fun igButton(label: String, size: Vec2.ByVal): Boolean
		external fun igSmallButton(label: String): Boolean
		external fun igInvisibleButton(str_id: String, size: Vec2.ByVal): Boolean
		external fun igImage(user_texture_id: Long, size: Vec2.ByVal, uv0: Vec2.ByVal, uv1: Vec2.ByVal, tint_col: Vec4.ByVal, border_col: Vec4.ByVal)
		external fun igCheckbox(label: String, v: Long): Boolean

		external fun igBeginCombo(label: String, preview_value: String?, flags: Int): Boolean
		external fun igEndCombo()

		external fun igSliderInt(label: String, v: Long, v_min: Int, v_max: Int, format: String): Boolean
		external fun igSliderFloat(label: String, v: Long, v_min: Float, v_max: Float, format: String, power: Float): Boolean

		external fun igSelectable(label: String, selected: Boolean, flags: Int, size: Vec2.ByVal): Boolean
		external fun igSelectable(label: String, p_selected: Long, flags: Int, size: Vec2.ByVal): Boolean

		external fun igListBoxHeaderInt(label: String, items_count: Int, height_in_items: Int): Boolean
		external fun igListBoxFooter()

		external fun igOpenPopup(str_id: String)
		external fun igBeginPopup(str_id: String, flags: Int): Boolean
		external fun igBeginPopupContextItem(str_id: String?, mouse_button: Int): Boolean
		external fun igBeginPopupContextWindow(str_id: String?, mouse_button: Int, also_over_items: Boolean): Boolean
		external fun igBeginPopupContextVoid(str_id: String?, mouse_button: Int): Boolean
		external fun igBeginPopupModal(name: String, p_open: Long, flags: Int): Boolean
		external fun igEndPopup()
		external fun igOpenPopupOnItemClick(str_id: String?, mouse_button: Int): Boolean
		external fun igIsPopupOpen(str_id: String): Boolean
		external fun igCloseCurrentPopup()

		external fun igIsItemHovered(flags: Int): Boolean
		external fun igIsItemActive(): Boolean
		external fun igIsItemFocused(): Boolean
		external fun igIsItemClicked(mouse_button: Int): Boolean
		external fun igIsItemVisible(): Boolean
		external fun igIsItemEdited(): Boolean
		external fun igIsItemDeactivated(): Boolean
		external fun igIsItemDeactivatedAfterEdit(): Boolean
		external fun igIsAnyItemHovered(): Boolean
		external fun igIsAnyItemActive(): Boolean
		external fun igIsAnyItemFocused(): Boolean
		external fun igGetItemRectMin(): Vec2.ByVal
		external fun igGetItemRectMax(): Vec2.ByVal
		external fun igGetItemRectSize(): Vec2.ByVal
		external fun igSetItemAllowOverlap()

		external fun igGetKeyIndex(imgui_key: Int): Int
		external fun igIsKeyDown(user_key_index: Int): Boolean
		external fun igIsKeyPressed(user_key_index: Int, repeat: Boolean): Boolean
		external fun igIsKeyReleased(user_key_index: Int): Boolean
		external fun igGetKeyPressedAmount(key_index: Int, repeat_delay: Float, rate: Float): Int
		external fun igIsMouseDown(button: Int): Boolean
		external fun igIsAnyMouseDown(): Boolean
		external fun igIsMouseClicked(button: Int, repeat: Boolean): Boolean
		external fun igIsMouseDoubleClicked(button: Int)
		external fun igIsMouseReleased(button: Int)
		external fun igIsMouseDragging(button: Int, lock_threhsold: Float)
		external fun igIsMouseHoveringRect(r_min: Vec2.ByVal, r_max: Vec2.ByVal, clip: Boolean): Boolean
		external fun igIsMousePosValid(mouse_pos: Vec2.ByRef?): Boolean
		external fun igGetMousePos(): Vec2.ByVal
		external fun igGetMousePosOnOpeningCurrentPopup(): Vec2.ByVal
		external fun igGetMouseDragDelta(button: Int, lock_threshold: Float): Vec2.ByVal
		external fun igResetMouseDragDelta(button: Int)
		// TODO: expose mouse cursor API
		external fun igCaptureKeyboardFromApp(want_capture_keyboard_value: Boolean)
		external fun igCaptureMouseFromApp(want_capture_keyboard_value: Boolean)

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

			class ByVal(x: Float = 0f, y: Float = 0f) : Vec2(x, y), Structure.ByValue {

				constructor(pos: Offset2D) : this(pos.x.toFloat(), pos.y.toFloat())
				fun toOffset() = Offset2D(x.toInt(), y.toInt())

				constructor(size: Extent2D) : this(size.width.toFloat(), size.height.toFloat())
				fun toExtent() = Extent2D(x.toInt(), y.toInt())

				constructor(v: Vector2f) : this(v.x, v.y)
				fun toVector(v: Vector2f) = v.set(x, y)
			}

			class ByRef(x: Float = 0f, y: Float = 0f) : Vec2(x, y), Structure.ByReference {

				constructor(pos: Offset2D) : this(pos.x.toFloat(), pos.y.toFloat())
				fun toOffset() = Offset2D(x.toInt(), y.toInt())

				constructor(size: Extent2D) : this(size.width.toFloat(), size.height.toFloat())
				fun toExtent() = Extent2D(x.toInt(), y.toInt())

				constructor(v: Vector2f) : this(v.x, v.y)
				fun toVector(v: Vector2f) = v.set(x, y)
			}
		}

		@Structure.FieldOrder("x", "y", "z", "w")
		sealed class Vec4(
			@JvmField var x: Float,
			@JvmField var y: Float,
			@JvmField var z: Float,
			@JvmField var w: Float
		) : Structure() {

			class ByVal(x: Float = 0f, y: Float = 0f, z: Float = 0f, w: Float = 0f) : Vec4(x, y, z, w), Structure.ByValue {

				constructor(color: ColorRGBA) : this(color.rf, color.gf, color.bf, color.af)
				fun toColorRGBA() = ColorRGBA.Float(x, y, z, w)
			}

			class ByRef(x: Float = 0f, y: Float = 0f, z: Float = 0f, w: Float = 0f) : Vec4(x, y, z, w), Structure.ByReference
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
		val sequence: Int,
		val queue: Queue,
		val descriptorPool: DescriptorPool
	) : AutoCloseable {

		val autoCloser = AutoCloser()
		fun <T:AutoCloseable> T.autoClose(): T = autoCloser.add(this)
		override fun close() = autoCloser.close()

		val device = queue.device

		val commandPool = device.commandPool(queue.family).autoClose()

		val imageDescriptors = ArrayList<ImageDescriptor>()
	}

	fun init(
		window: Window,
		queue: Queue,
		descriptorPool: DescriptorPool,
		renderPass: RenderPass
	) {
		// was anything already initialized from before?
		var sequence = 0
		stateOrNull?.let {

			// yup, get the sequence of the old init
			sequence = it.sequence + 1

			// clean it up before init'ing new stuff
			close()
		}

		// init native side
		val installCallbacks = sequence == 0
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
		stateOrNull = State(sequence, queue, descriptorPool)
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

	class ImageDescriptor(
		internal val sequence: Int,
		internal val _layout: DescriptorSetLayout,
		internal val _descriptorSet: DescriptorSet,
		val extent: Extent3D
	) : AutoCloseable {

		override fun close() {
			_layout.close()
		}

		private fun check() {
			if (this.sequence != stateOrNull?.sequence) {
				throw IllegalStateException("ImageDescriptor has expired because Imgui was re-initialized")
			}
		}

		internal val layout get() = check().run { _layout }
		internal val descriptorSet get() = check().run { _descriptorSet }
	}

	fun imageDescriptor(view: Image.View, sampler: Sampler): ImageDescriptor = state.run {

		val binding = DescriptorSetLayout.Binding(
			binding = 0,
			type = DescriptorType.CombinedImageSampler,
			stages = IntFlags.of(ShaderStage.Fragment)
		)

		val descriptorSetLayout = device.descriptorSetLayout(listOf(binding))
		val descriptorSet = descriptorPool.allocate(listOf(descriptorSetLayout))[0]

		device.updateDescriptorSets(
			writes = listOf(
				descriptorSet.address(binding).write(
					images = listOf(
						DescriptorSet.ImageInfo(
							sampler = sampler,
							view = view,
							layout = Image.Layout.ShaderReadOnlyOptimal
						)
					)
				)
			)
		)

		return ImageDescriptor(state.sequence, descriptorSetLayout, descriptorSet, view.image.extent)
	}

	object io {

		fun getBool(offset: Int) = MemoryUtil.memGetBoolean(native.igGetIO() + offset)
		fun setBool(offset: Int, value: Boolean) = MemoryUtil.memPutByte(native.igGetIO() + offset, if (value) 1 else 0)

		fun getFloat(offset: Int) = MemoryUtil.memGetFloat(native.igGetIO() + offset)
		fun setFloat(offset: Int, value: Float) = MemoryUtil.memPutFloat(native.igGetIO() + offset, value)

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
			val height: Float get() = getFloat(8 + 4)
		}
		val deltaTime: Float get() = getFloat(16)
		val frameRate: Float get() = getFloat(928)

		var configWindowsMoveFromTitleBarOnly
			get() = getBool(204)
			set(value) = setBool(204, value)

		// mouse values
		object mouse {
			val x: Float get() = getFloat(296)
			val y: Float get() = getFloat(296 + 4)
			val wheel: Float get() = getFloat(312)
			object buttonDown {
				operator fun get(i: Int) = getBool(304 + i)
			}
		}
	}
}
