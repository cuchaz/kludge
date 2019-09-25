/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.imgui

import cuchaz.kludge.tools.*
import cuchaz.kludge.imgui.Imgui.native.Vec2
import cuchaz.kludge.imgui.Imgui.native.Vec4
import cuchaz.kludge.vulkan.*
import org.joml.Vector2f
import org.joml.Vector2fc
import java.nio.ByteBuffer


class Commands internal constructor() {

	private val n = Imgui.native


	fun showDemoWindow(open: Ref<Boolean>? = null) {
		memstack { mem ->
			val pOpen = open?.toBuf(mem)
			n.igShowDemoWindow(pOpen.address)
			open?.fromBuf(pOpen)
		}
	}

	fun showAboutWindow(open: Ref<Boolean>? = null) {
		memstack { mem ->
			val pOpen = open?.toBuf(mem)
			n.igShowAboutWindow(pOpen.address)
			open?.fromBuf(pOpen)
		}
	}

	fun showMetricsWindow(open: Ref<Boolean>? = null) {
		memstack { mem ->
			val pOpen = open?.toBuf(mem)
			n.igShowMetricsWindow(pOpen.address)
			open?.fromBuf(pOpen)
		}
	}

	fun showUserGuide() = n.igShowUserGuide()


	enum class BeginFlags(override val value: Int) : IntFlags.Bit {
		None(0),
		NoTitleBar(1 shl 0),
		NoResize(1 shl 1),
		NoMove(1 shl 2),
		NoScrollbar(1 shl 3),
		NoScrollWithMouse(1 shl 4),
		NoCollapse(1 shl 5),
		AlwaysAutoResize(1 shl 6),
		NoBackground(1 shl 7),
		NoSavedSettings(1 shl 8),
		NoMouseInputs(1 shl 9),
		MenuBar(1 shl 10),
		HorizontalScrollBar(1 shl 11),
		NoFocusOnAppearing(1 shl 12),
		NoBringToFrontOnFocus(1 shl 13),
		AlwaysVerticalScrollbar(1 shl 14),
		AlwaysHorizontalScrollbar(1 shl 15),
		AlwaysUseWindowPadding(1 shl 16),
		NoNavInputs(1 shl 18),
		NoNavFocus(1 shl 19),
		UnsavedDocument(1 shl 20),
		NoNav(NoNavInputs.value or NoNavFocus.value),
		NoDecoration(NoTitleBar.value or NoResize.value or NoScrollbar.value or NoCollapse.value),
		NoInputs(NoMouseInputs.value or NoNavInputs.value or NoNavFocus.value),
		NavFlattened(1 shl 23),
		ChildWindow(1 shl 24),
		Tooltip(1 shl 25),
		Popup(1 shl 26),
		Modal(1 shl 27),
		ChildMenu(1 shl 28)
	}

	fun begin(
		name: String,
		open: Ref<Boolean>? = null,
		flags: IntFlags<BeginFlags> = IntFlags(0)
	): Boolean {
		memstack { mem ->
			val pOpen = open?.toBuf(mem)
			return n.igBegin(name, pOpen?.address ?: 0, flags.value)
				.also {
					open?.fromBuf(pOpen)
				}
		}
	}

	fun end() = n.igEnd()


	fun beginChild(
		id: String,
		width: Float,
		height: Float,
		border: Boolean = false,
		flags: IntFlags<BeginFlags> = IntFlags(0)
	) = n.igBeginChild(id, Vec2.ByVal(width, height), border, flags.value)
	fun beginChild(
		id: String,
		size: Extent2D = Extent2D(0, 0),
		border: Boolean = false,
		flags: IntFlags<BeginFlags> = IntFlags(0)
	) = n.igBeginChild(id, Vec2.ByVal(size), border, flags.value)

	fun endChild() = n.igEndChild()

	enum class FocusedFlags(override val value: Int) : IntFlags.Bit {
		ChildWindows(1 shl 0),
		RootWindow(1 shl 1),
		AnyWindow(1 shl 2),
		RootAndChildWindows(RootWindow.value or ChildWindows.value)
	}

	enum class HoveredFlags(override val value: Int) : IntFlags.Bit {
		None(0),
		ChildWindows(1 shl 0),
		RootWindow(1 shl 1),
		AnyWindow(1 shl 2),
		AllowWhenBlockedByPopup(1 shl 3),
		AllowWhenBlockedByModal(1 shl 4),
		AllowWhenBlockedByActiveItem(1 shl 5),
		AllowWhenOverlapped(1 shl 6),
		AllowWhenDisabled(1 shl 7),
		RectOnly(AllowWhenBlockedByPopup.value or AllowWhenBlockedByActiveItem.value or AllowWhenOverlapped.value),
		RootAndChildWindows(RootWindow.value or ChildWindows.value)
	}

	fun isWindowAppearing() = n.igIsWindowAppearing()
	fun isWindowCollapsed() = n.igIsWindowCollapsed()
	fun isWindowFocused(flags: IntFlags<FocusedFlags> = IntFlags(0)) = n.igIsWindowFocused(flags.value)
	fun isWindowHovered(flags: IntFlags<HoveredFlags> = IntFlags(0)) = n.igIsWindowHovered(flags.value)
	fun getWindowPos(out: Vector2f) = n.igGetWindowPos().toVector(out)
	fun getWindowSize(out: Vector2f) = n.igGetWindowSize().toVector(out)
	fun getWindowWidth() = n.igGetWindowWidth()
	fun getWindowHeight() = n.igGetWindowHeight()
	fun getContentRegionMax(out: Vector2f) = n.igGetContentRegionMax().toVector(out)
	fun getContentRegionAvail(out: Vector2f) = n.igGetContentRegionAvail().toVector(out)
	fun getContentRegionAvailWidth() = n.igGetContentRegionAvailWidth()
	fun getWindowContentRegionMin(out: Vector2f) = n.igGetWindowContentRegionMin().toVector(out)
	fun getWindowContentRegionMax(out: Vector2f) = n.igGetWindowContentRegionMax().toVector(out)
	fun getWindowContentRegionWidth() = n.igGetWindowContentRegionWidth()


	enum class Cond(override val value: Int) : IntFlags.Bit {
		Always(1 shl 0),
		Once(1 shl 1),
		FirstUseEver(1 shl 2),
		Appearing(1 shl 3)
	}

	fun setNextWindowPos(x: Float, y: Float, cond: Cond = Cond.Always, pivotx: Float = 0f, pivoty: Float = 0f) =
		n.igSetNextWindowPos(Vec2.ByVal(x, y), cond.value, Vec2.ByVal(pivotx, pivoty))
	fun setNextWindowPos(pos: Offset2D, cond: Cond = Cond.Always, pivot: Offset2D = Offset2D(0, 0)) =
		n.igSetNextWindowPos(Vec2.ByVal(pos), cond.value, Vec2.ByVal(pivot))

	fun setNextWindowSize(width: Float, height: Float, cond: Cond = Cond.Always) =
		n.igSetNextWindowSize(Vec2.ByVal(width, height), cond.value)
	fun setNextWindowSize(size: Extent2D, cond: Cond = Cond.Always) =
		n.igSetNextWindowSize(Vec2.ByVal(size), cond.value)

	fun setNextWindowSizeConstraints(xmin: Float, ymin: Float, xmax: Float, ymax: Float) =
		n.igSetNextWindowSizeConstraints(Vec2.ByVal(xmin, ymin), Vec2.ByVal(xmax, ymax), 0, 0)
	fun setNextWindowSizeConstraints(min: Extent2D, max: Extent2D) =
		n.igSetNextWindowSizeConstraints(Vec2.ByVal(min), Vec2.ByVal(max), 0, 0)

	fun setNextWindowContentSize(width: Float, height: Float) =
		n.igSetNextWindowContentSize(Vec2.ByVal(width, height))
	fun setNextWindowContentSize(size: Extent2D) =
		n.igSetNextWindowContentSize(Vec2.ByVal(size))

	fun setNextWindowCollapsed(collapsed: Boolean, cond: Cond = Cond.Always) =
		n.igSetNextWindowCollapsed(collapsed, cond.value)

	fun setNextWindowFocus() = n.igSetNextWindowFocus()
	fun setNextWindowBgAlpha(alpha: Float) = n.igSetNextWindowBgAlpha(alpha)
	fun setWindowFontScale(scale: Float) = n.igSetWindowFontScale(scale)

	fun setWindowPos(name: String, x: Float, y: Float, cond: Cond = Cond.Always) =
		n.igSetWindowPosStr(name, Vec2.ByVal(x, y), cond.value)
	fun setWindowPos(name: String, pos: Offset2D, cond: Cond = Cond.Always) =
		n.igSetWindowPosStr(name, Vec2.ByVal(pos), cond.value)

	fun setWindowSize(name: String, width: Float, height: Float, cond: Cond = Cond.Always) =
		n.igSetWindowSizeStr(name, Vec2.ByVal(width, height), cond.value)
	fun setWindowSize(name: String, size: Extent2D, cond: Cond = Cond.Always) =
		n.igSetWindowSizeStr(name, Vec2.ByVal(size), cond.value)

	fun setWindowCollapsed(name: String, collapsed: Boolean, cond: Cond = Cond.Always) =
		n.igSetWindowCollapsedStr(name, collapsed, cond.value)

	fun setWindowFocus(name: String) =
		n.igSetWindowFocusStr(name)


	enum class StyleColor {
		Text,
		TextDisabled,
		WindowBg,
		ChildBg,
		PopupBg,
		Border,
		BorderShadow,
		FrameBg,
		FrameBgHovered,
		FrameBgActive,
		TitleBg,
		TitleBgActive,
		TitleBgCollapsed,
		MenuBarBg,
		ScrollbarBg,
		ScrollbarGrab,
		ScrollbarGrabHovered,
		ScrollbarGrabActive,
		CheckMark,
		SliderGrab,
		SliderGrabActive,
		Button,
		ButtonHovered,
		ButtonActive,
		Header,
		HeaderHovered,
		HeaderActive,
		Separator,
		SeparatorHovered,
		SeparatorActive,
		ResizeGrip,
		ResizeGripHovered,
		ResizeGripActive,
		Tab,
		TabHovered,
		TabActive,
		TabUnfocused,
		TabUnfocusedActive,
		PlotLines,
		PlotLinesHovered,
		PlotHistogram,
		PlotHistogramHovered,
		TextSelectedBg,
		DragDropTarget,
		NavHighlight,
		NavWindowingHighlight,
		NavWindowingDimBg,
		ModalWindowDimBg,
	}
	fun pushStyleColor(idx: StyleColor, color: ColorRGBA) = n.igPushStyleColor(idx.ordinal, Vec4.ByVal(color))
	fun popStyleColor(count: Int = 1) = n.igPopStyleColor(count)

	enum class StyleVar {
		Alpha,
		WindowPadding,
		WindowRounding,
		WindowBorderSize,
		WindowMinSize,
		WindowTitleAlign,
		ChildRounding,
		ChildBorderSize,
		PopupRounding,
		PopupBorderSize,
		FramePadding,
		FrameRounding,
		FrameBorderSize,
		ItemSpacing,
		ItemInnerSpacing,
		IndentSpacing,
		ScrollbarSize,
		ScrollbarRounding,
		GrabMinSize,
		GrabRounding,
		TabRounding,
		ButtonTextAlign
	}
	fun pushStyleVar(idx: StyleVar, value: Float) = n.igPushStyleVarFloat(idx.ordinal, value)
	fun pushStyleVar(idx: StyleVar, value: Vector2fc) = n.igPushStyleVarVec2(idx.ordinal, Vec2.ByVal(value))
	fun pushStyleVar(idx: StyleVar, x: Float, y: Float) = n.igPushStyleVarVec2(idx.ordinal, Vec2.ByVal(x, y))
	fun popStyleVar(count: Int = 1) = n.igPopStyleVar(count)
	fun getStyleColor(idx: StyleVar) = n.igGetStyleColorVec4(idx.ordinal).toColorRGBA()

	fun pushItemWidth(width: Float) = n.igPushItemWidth(width)
	fun popItemWidth() = n.igPopItemWidth()
	fun calcItemWidth()= n.igCalcItemWidth()
	fun pushTextWrapPos(pos: Float = 0f) = n.igPushTextWrapPos(pos)
	fun popTextWrapPos() = n.igPopTextWrapPos()
	fun pushAllowKeyboardFocus(allow: Boolean) = n.igPushAllowKeyboardFocus(allow)
	fun popAllowKeyboardFocus() = n.igPopAllowKeyboardFocus()
	fun pushButtonRepeat(repeat: Boolean) = n.igPushButtonRepeat(repeat)
	fun popButtonRepeat() = n.igPopButtonRepeat()


	fun separator() = n.igSeparator()
	fun sameLine(pos: Float = 0f, spacing: Float = -1f) = n.igSameLine(pos, spacing)
	fun newLine() = n.igNewLine()
	fun spacing() = n.igSpacing()
	fun dummy(width: Float, height: Float) = n.igDummy(Vec2.ByVal(width, height))
	fun dummy(size: Vector2fc) = n.igDummy(Vec2.ByVal(size))
	fun indent(indent: Float = 0f) = n.igIndent(indent)
	fun unindent(indent: Float = 0f) = n.igUnindent(indent)
	fun beginGroup() = n.igBeginGroup()
	fun endGroup() = n.igEndGroup()
	fun getCursorPos(out: Vector2f) = n.igGetCursorPos().toVector(out)
	fun getCursorPosX() = n.igGetCursorPosX()
	fun getCursorPosY() = n.igGetCursorPosY()
	fun setCursorPos(x: Float, y: Float) = n.igSetCursorPos(Vec2.ByVal(x, y))
	fun setCursorPos(pos: Vector2fc) = n.igSetCursorPos(Vec2.ByVal(pos))
	fun setCursorPosX(x: Float) = n.igSetCursorPosX(x)
	fun setCursorPosY(y: Float) = n.igSetCursorPosY(y)
	fun getCursorStartPos(out: Vector2f) = n.igGetCursorStartPos().toVector(out)
	fun getCursorScreenPos(out: Vector2f) = n.igGetCursorScreenPos().toVector(out)
	fun setCursorScreenPos(x: Float, y: Float) = n.igSetCursorScreenPos(Vec2.ByVal(x, y))
	fun setCursorScreenPos(pos: Vector2fc) = n.igSetCursorScreenPos(Vec2.ByVal(pos))
	fun alignTextToFramePadding() = n.igAlignTextToFramePadding()
	fun getTextLineHeight() = n.igGetTextLineHeight()
	fun getTextLineHeightWithSpacing() = n.igGetTextLineHeightWithSpacing()
	fun getFrameHeight() = n.igGetFrameHeight()
	fun getFrameHeightWithSpacing() = n.igGetFrameHeightWithSpacing()


	fun textUnformatted(text: String, textEnd: String? = null) = n.igTextUnformatted(text, textEnd)
	fun text(text: String) = n.igText(text)
	fun textColored(color: ColorRGBA, text: String) = n.igTextColored(Vec4.ByVal(color), text)
	fun textDisabled(text: String) = n.igTextDisabled(text)
	fun textWrapped(text: String) = n.igTextWrapped(text)
	fun labelText(label: String, text: String) = n.igLabelText(label, text)
	fun bulletText(text: String) = n.igBulletText(text)


	fun checkbox(label: String, isChecked: Ref<Boolean>): Boolean {
		memstack { mem ->
			val pChecked = isChecked.toBuf(mem)
			return n.igCheckbox(label, pChecked.address)
				.also {
					isChecked.fromBuf(pChecked)
				}
		}
	}

	fun button(label: String, width: Float = 0f, height: Float = 0f) =
		n.igButton(label, Vec2.ByVal(width, height))
	fun button(label: String, size: Extent2D) =
		n.igButton(label, Vec2.ByVal(size))
	fun button(label: String, size: Vector2fc) =
		n.igButton(label, Vec2.ByVal(size))

	fun smallButton(label: String) = n.igSmallButton(label)

	fun invisibleButton(id: String, width: Float, height: Float) =
		n.igInvisibleButton(id, Vec2.ByVal(width, height))
	fun invisibleButton(id: String, size: Extent2D) =
		n.igInvisibleButton(id, Vec2.ByVal(size))
	fun invisibleButton(id: String, size: Vector2fc) =
		n.igInvisibleButton(id, Vec2.ByVal(size))

	fun image(
		image: Imgui.ImageDescriptor,
		width: Float, height: Float,
		uv0x: Float = 0f, uv0y: Float = 0f,
		uv1x: Float = 1f, uv1y: Float = 1f,
		tintr: Float = 1f, tintg: Float = 1f, tintb: Float = 1f, tinta: Float = 1f,
		borderr: Float = 0f, borderg: Float = 0f, borderb: Float = 0f, bordera: Float = 0f
	) = n.igImage(
		image.descriptorSet.id,
		Vec2.ByVal(width, height),
		Vec2.ByVal(uv0x, uv0y),
		Vec2.ByVal(uv1x, uv1y),
		Vec4.ByVal(tintr, tintg, tintb, tinta),
		Vec4.ByVal(borderr, borderg, borderb, bordera)
	)

	fun image(
		image: Imgui.ImageDescriptor,
		size: Extent2D = image.extent.to2D(),
		region: Rect2D = Rect2D(Offset2D(0, 0), size),
		tintColor: ColorRGBA = ColorRGBA.Float(1f, 1f, 1f, 1f),
		borderColor: ColorRGBA = ColorRGBA.Float(0f, 0f, 0f, 0f)
	) = n.igImage(
		image.descriptorSet.id,
		Vec2.ByVal(size),
		Vec2.ByVal(
			region.offset.x.toFloat()/image.extent.width.toFloat(),
			region.offset.y.toFloat()/image.extent.height.toFloat()
		),
		Vec2.ByVal(
			(region.offset.x + region.extent.width).toFloat()/image.extent.width.toFloat(),
			(region.offset.y + region.extent.height).toFloat()/image.extent.height.toFloat()
		),
		Vec4.ByVal(tintColor),
		Vec4.ByVal(borderColor)
	)

	enum class ComboFlags(override val value: Int) : IntFlags.Bit {
		PopupAlignLeft(1 shl 0),
		HeightSmall(1 shl 1),
		HeightRegular(1 shl 2),
		HeightLarge(1 shl 3),
		HeightLargest(1 shl 4),
		NoArrowButton(1 shl 5),
		NoPreview(1 shl 6)
	}

	fun beginCombo(
		label: String,
		preview: String? = null,
		flags: IntFlags<ComboFlags> = IntFlags(0)
	) = n.igBeginCombo(label, preview, flags.value)

	fun endCombo() = n.igEndCombo()

	fun sliderInt(
		label: String,
		value: Ref<Int>,
		min: Int,
		max: Int,
		format: String = "%d"
	): Boolean {
		memstack { mem ->
			val pVal = value.toBuf(mem)
			return n.igSliderInt(label, pVal.address, min, max, format)
				.also {
					value.fromBuf(pVal)
				}
		}
	}

	fun sliderFloat(
		label: String,
		value: Ref<Float>,
		min: Float,
		max: Float,
		format: String = "%f",
		power: Float = 1f
	): Boolean {
		memstack { mem ->
			val pVal = value.toBuf(mem)
			return n.igSliderFloat(label, pVal.address, min, max, format, power)
				.also {
					value.fromBuf(pVal)
				}
		}
	}


	enum class InputTextFlags(override val value: Int) : IntFlags.Bit {
		CharsDecimal(1 shl 0),
		CharsHexadecimal(1 shl 1),
		CharsUppercase(1 shl 2),
		CharsNoBlank(1 shl 3),
		AutoSelectAll(1 shl 4),
		EnterReturnsTrue(1 shl 5),
		// TODO: implement callbacks?
		//CallbackCompletion(1 shl 6),
		//CallbackHistory(1 shl 7),
		//CallbackAlways(1 shl 8),
		//CallbackCharFilter(1 shl 9),
		AllowTabInput(1 shl 10),
		CtrlEnterForNewLine(1 shl 11),
		NoHorizontalScroll(1 shl 12),
		AlwaysInsertMode(1 shl 13),
		ReadOnly(1 shl 14),
		Password(1 shl 15),
		NoUndoRedo(1 shl 16),
		CharsScientific(1 shl 17),
		//CallbackResize(1 shl 18)
	}

	class TextBuffer(val bytes: Int) {

		companion object {

			fun of(text: String): TextBuffer {
				val utf8 = text.toByteArray(Charsets.UTF_8)
				return TextBuffer(utf8.size + 1).apply {
					buf.put(utf8)
					buf.put(0) // add a null terminator
					buf.rewind()
				}
			}
		}

		internal val buf = ByteBuffer.allocateDirect(bytes) // yeah, this needs to be direct
			.apply {
				// zero the buffer on first use
				// TODO: this is probably very slow, but there don't seem to be any better (portable) alternatives
				// see: https://stackoverflow.com/questions/11197875/fast-erase-not-clear-a-bytebuffer-in-java
				for (i in 0 until bytes) {
					put(0)
				}
				flip()
			}

		fun hasRoomFor(value: String) = value.toByteArray(Charsets.UTF_8).size + 1 <= bytes

		var text: String
			get() {

				// scan for the null terminator to set the buffer bounds
				buf.clear()
				var pos = 0
				while (pos < buf.capacity && buf.get(pos) != 0.toByte()) {
					pos++
				}
				buf.limit = pos

				return Charsets.UTF_8.decode(buf).toString()
			}
			set(value) {
				val utf8 = value.toByteArray(Charsets.UTF_8)
				if (utf8.size + 1 > bytes) {
					throw IllegalArgumentException("not enough room for string: needs ${utf8.size + 1} bytes, but only have $bytes")
				}
				buf.clear()
				buf.put(utf8)
				buf.put(0) // add a null terminator
				buf.rewind()
			}

		override fun toString() = text
	}

	fun inputText(
		label: String,
		text: TextBuffer,
		flags: IntFlags<InputTextFlags> = IntFlags(0)
	) = n.igInputText(label, text.buf.address, text.bytes.toLong(), flags.value, 0L, 0L)

	fun inputTextMultiline(
		label: String,
		text: TextBuffer,
		size: Extent2D,
		flags: IntFlags<InputTextFlags> = IntFlags(0)
	) = n.igInputTextMultiline(label, text.buf.address, text.bytes.toLong(), Vec2.ByVal(size), flags.value, 0L, 0L)
	fun inputTextMultiline(
		label: String,
		text: TextBuffer,
		width: Float,
		height: Float,
		flags: IntFlags<InputTextFlags> = IntFlags(0)
	) = n.igInputTextMultiline(label, text.buf.address, text.bytes.toLong(), Vec2.ByVal(width, height), flags.value, 0L, 0L)


	enum class SelectableFlags(override val value: Int): IntFlags.Bit {
		DontClosePopups(1 shl 0),
		SpanAllColumns(1 shl 1),
		AllowDoubleClick(1 shl 2),
		Disabled(1 shl 3)
	}

	fun selectable(
		label: String,
		isSelected: Boolean,
		flags: IntFlags<SelectableFlags> = IntFlags(0),
		width: Float = 0f,
		height: Float = 0f
	) = n.igSelectable(label, isSelected, flags.value, Vec2.ByVal(width, height))

	fun selectable(
		label: String,
		isSelected: Boolean,
		flags: IntFlags<SelectableFlags> = IntFlags(0),
		size: Extent2D = Extent2D(0, 0)
	) = n.igSelectable(label, isSelected, flags.value, Vec2.ByVal(size))

	fun selectable(
		label: String,
		isSelected: Ref<Boolean>,
		flags: IntFlags<SelectableFlags> = IntFlags(0),
		width: Float = 0f,
		height: Float = 0f
	): Boolean {
		memstack { mem ->
			val pSelected = isSelected.toBuf(mem)
			return n.igSelectableBoolPtr(label, pSelected.address, flags.value, Vec2.ByVal(width, height))
				.also {
					isSelected.fromBuf(pSelected)
				}
		}
	}

	fun selectable(
		label: String,
		isSelected: Ref<Boolean>,
		flags: IntFlags<SelectableFlags> = IntFlags(0),
		size: Extent2D = Extent2D(0, 0)
	): Boolean {
		memstack { mem ->
			val pSelected = isSelected.toBuf(mem)
			return n.igSelectableBoolPtr(label, pSelected.address, flags.value, Vec2.ByVal(size))
				.also {
					isSelected.fromBuf(pSelected)
				}
		}
	}

	fun listBoxHeader(
		label: String,
		itemsCount: Int,
		heightInItems: Int = -1
	) = n.igListBoxHeaderInt(label, itemsCount, heightInItems)

	fun listBoxFooter() = n.igListBoxFooter()


	fun beginMainMenuBar() = n.igBeginMainMenuBar()
	fun endMainMenuBar() = n.igEndMainMenuBar()
	fun beginMenuBar() = n.igBeginMenuBar()
	fun endMenuBar() = n.igEndMenuBar()
	fun beginMenu(label: String, enabled: Boolean = true) = n.igBeginMenu(label, enabled)
	fun endMenu() = n.igEndMenu()
	fun menuItem(label: String, shortcut: String? = null) =
		menuItem(label, shortcut, false)
	fun menuItem(label: String, shortcut: String? = null, selected: Boolean = false, enabled: Boolean = true) =
		n.igMenuItemBool(label, shortcut, selected, enabled)
	fun menuItem(label: String, shortcut: String? = null, selected: Ref<Boolean>? = null, enabled: Boolean = true): Boolean {
		memstack { mem ->
			val pSelected = selected?.toBuf(mem)
			return n.igMenuItemBoolPtr(label, shortcut, pSelected?.address ?: 0, enabled)
				.also {
					selected?.fromBuf(pSelected)
				}
		}
	}

	fun beginTooltip() = n.igBeginTooltip()
	fun endTooltip() = n.igEndTooltip()
	fun setTooltip(text: String) = n.igSetTooltip(text)


	fun openPopup(id: String) = n.igOpenPopup(id)
	fun beginPopup(id: String, flags: IntFlags<BeginFlags> = IntFlags(0)) = n.igBeginPopup(id, flags.value)
	fun beginPopupContextItem(id: String? = null, mouseButton: Int = 1) = n.igBeginPopupContextItem(id, mouseButton)
	fun beginPopupContextWindow(id: String? = null, mouseButton: Int = 1, alsoOverItems: Boolean = true) =
		n.igBeginPopupContextWindow(id, mouseButton, alsoOverItems)
	fun beginPopupContextVoid(id: String? = null, mouseButton: Int = 1) = n.igBeginPopupContextVoid(id, mouseButton)
	fun beginPopupModal(name: String, open: Ref<Boolean>? = null, flags: IntFlags<BeginFlags> = IntFlags(0)): Boolean {
		memstack { mem ->
			val pOpen = open?.toBuf(mem)
			return n.igBeginPopupModal(name, pOpen?.address ?: 0, flags.value)
				.also {
					open?.fromBuf(pOpen)
				}
		}
	}
	fun endPopup() = n.igEndPopup()
	fun openPopupOnItemClick(id: String? = null, mouseButton: Int = 1) = n.igOpenPopupOnItemClick(id, mouseButton)
	fun isPopupOpen(id: String) = n.igIsPopupOpen(id)
	fun closeCurrentPopup() = n.igCloseCurrentPopup()


	fun columns(count: Int = 1, id: String? = null, border: Boolean = true) = n.igColumns(count, id, border)
	fun nextColumn() = n.igNextColumn()
	fun getColumnIndex() = n.igGetColumnIndex()
	fun getColumnWidth(index: Int = -1) = n.igGetColumnWidth(index)
	fun setColumnWidth(index: Int, width: Float) = n.igSetColumnWidth(index, width)
	fun getColumnOffset(index: Int = -1) = n.igGetColumnWidth(index)
	fun setColumnOffset(index: Int, offset: Float) = n.igSetColumnOffset(index, offset)
	fun getColumnsCount() = n.igGetColumnsCount()


	fun isItemHovered(flags: IntFlags<HoveredFlags> = IntFlags(0)) = n.igIsItemHovered(flags.value)
	fun isItemActive() = n.igIsItemActive()
	fun isItemFocused() = n.igIsItemFocused()
	fun isItemClicked(mouseButton: Int) = n.igIsItemClicked(mouseButton)
	fun isItemVisible() = n.igIsItemVisible()
	fun isItemEdited() = n.igIsItemEdited()
	fun isItemDeactivated() = n.igIsItemDeactivated()
	fun isItemDeactivatedAfterEdit() = n.igIsItemDeactivatedAfterEdit()
	fun isAnyItemHovered() = n.igIsAnyItemHovered()
	fun isAnyItemActive() = n.igIsAnyItemActive()
	fun isAnyItemFocused() = n.igIsAnyItemFocused()
	fun getItemRectMin(out: Vector2f) = n.igGetItemRectMin().toVector(out)
	fun getItemRectMax(out: Vector2f) = n.igGetItemRectMax().toVector(out)
	fun getItemRectSize(out: Vector2f) = n.igGetItemRectSize().toVector(out)
	fun setItemAllowOverlap() = n.igSetItemAllowOverlap()


	enum class Key {
		Tab,
		LeftArrow,
		RightArrow,
		UpArrow,
		DownArrow,
		PageUp,
		PageDown,
		Home,
		End,
		Insert,
		Delete,
		Backspace,
		Space,
		Enter,
		Escape,
		A,
		C,
		V,
		X,
		Y,
		Z
	}

	fun getKeyIndex(imguiKey: Key): Int = n.igGetKeyIndex(imguiKey.ordinal)
	fun isKeyDown(key: Int) = n.igIsKeyDown(key)
	fun isKeyPressed(key: Int, repeat: Boolean = true) = n.igIsKeyPressed(key, repeat)
	fun isKeyReleased(key: Int) = n.igIsKeyReleased(key)
	fun getKeyPressedAmount(key: Int, repeatDelay: Float, rate: Float) = n.igGetKeyPressedAmount(key, repeatDelay, rate)
	fun isMouseDown(button: Int) = n.igIsMouseDown(button)
	fun isAnyMouseDown() = n.igIsAnyMouseDown()
	fun isMouseClicked(button: Int, repeat: Boolean = false) = n.igIsMouseClicked(button, repeat)
	fun isMouseDoubleClicked(button: Int) = n.igIsMouseDoubleClicked(button)
	fun isMouseReleased(button: Int) = n.igIsMouseReleased(button)
	fun isMouseDragging(button: Int, lockThreshold: Float = -1f) = n.igIsMouseDragging(button, lockThreshold)

	fun isMouseHoveringRect(minx: Float, miny: Float, maxx: Float, maxy: Float, clip: Boolean = true) =
		n.igIsMouseHoveringRect(Vec2.ByVal(minx, miny), Vec2.ByVal(maxx, maxy), clip)
	fun isMouseHoveringRect(min: Vector2fc, max: Vector2fc, clip: Boolean = true) =
		n.igIsMouseHoveringRect(Vec2.ByVal(min), Vec2.ByVal(max), clip)
	fun isMouseHoveringRect(rect: Rect2D, clip: Boolean = true) =
		n.igIsMouseHoveringRect(Vec2.ByVal(rect.offset), Vec2.ByVal(rect.xmax.toFloat(), rect.ymax.toFloat()), clip)

	fun isMousePosValid(pos: Offset2D? = null) = n.igIsMousePosValid(pos?.let { Vec2.ByRef(it) })
	fun isMousePosValid(pos: Vector2fc? = null) = n.igIsMousePosValid(pos?.let { Vec2.ByRef(it) })

	fun getMousePos(out: Vector2f) = n.igGetMousePos().toVector(out)
	fun getMousePosOnOpeningCurrentPopup(out: Vector2f) = n.igGetMousePosOnOpeningCurrentPopup().toVector(out)
	fun getMouseDragDelta(button: Int, out: Vector2f, lockThreshold: Float = -1f) = n.igGetMouseDragDelta(button, lockThreshold).toVector(out)
	fun resetMouseDragDelta(button: Int) = n.igResetMouseDragDelta(button)
	fun captureKeyboardFromApp(want: Boolean = true) = n.igCaptureKeyboardFromApp(want)
	fun captureMouseFromApp(want: Boolean = true) = n.igCaptureMouseFromApp(want)
}
