/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.imgui

import cuchaz.kludge.tools.*
import cuchaz.kludge.imgui.Imgui.native.Vec2
import cuchaz.kludge.imgui.Imgui.native.Vec4
import cuchaz.kludge.vulkan.*


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
		Tooltop(1 shl 25),
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

	// TODO: setNextWindowSizeConstraints?

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


	fun separator() = n.igSeparator()
	fun sameLine(pos: Float = 0f, spacing: Float = -1f) = n.igSameLine(pos, spacing)
	fun newLine() = n.igNewLine()
	fun spacing() = n.igSpacing()
	fun dummy(width: Float, height: Float) = n.igDummy(Vec2.ByVal(width, height))
	fun dummy(size: Extent2D) = n.igDummy(Vec2.ByVal(size))
	fun indent(indent: Float = 0f) = n.igIndent(indent)
	fun unindent(indent: Float = 0f) = n.igUnindent(indent)
	fun beginGroup() = n.igBeginGroup()
	fun endGroup() = n.igEndGroup()
	fun getCursorPos() = n.igGetCursorPos().toOffset()
	fun getCursorPosX() = n.igGetCursorPosX()
	fun getCursorPosY() = n.igGetCursorPosY()
	fun setCursorPos(x: Float, y: Float) = n.igSetCursorPos(Vec2.ByVal(x, y))
	fun setCursorPos(pos: Offset2D) = n.igSetCursorPos(Vec2.ByVal(pos))
	fun setCursorPosX(x: Float) = n.igSetCursorPosX(x)
	fun setCursorPosY(y: Float) = n.igSetCursorPosY(y)
	fun getCursorStartPos() = n.igGetCursorStartPos().toOffset()
	fun getCursorStartPosX() = n.igGetCursorStartPos().x
	fun getCursorStartPosY() = n.igGetCursorStartPos().y
	fun getCursorScreenPos() = n.igGetCursorScreenPos().toOffset()
	fun getCursorScreenPosX() = n.igGetCursorScreenPos().x
	fun getCursorScreenPosY() = n.igGetCursorScreenPos().y
	fun setCursorScreenPos(x: Float, y: Float) = n.igSetCursorScreenPos(Vec2.ByVal(x, y))
	fun setCursorScreenPos(pos: Offset2D) = n.igSetCursorScreenPos(Vec2.ByVal(pos))
	fun alignTextToFramePadding() = n.igAlignTextToFramePadding()
	fun getTextLineHeight() = n.igGetTextLineHeight()
	fun getTextLineHeightWithSpacing() = n.igGetTextLineHeightWithSpacing()
	fun getFrameHeight() = n.igGetFrameHeight()
	fun getFrameHeightWithSpacing() = n.igGetFrameHeightWithSpacing()


	fun text(text: String) = n.igText(text)

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

	fun smallButton(label: String) = n.igSmallButton(label)

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
			return n.igSelectable(label, pSelected.address, flags.value, Vec2.ByVal(width, height))
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
			return n.igSelectable(label, pSelected.address, flags.value, Vec2.ByVal(size))
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
}
