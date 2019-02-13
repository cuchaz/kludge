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

	fun end() {
		n.igEnd()
	}

	enum class Cond(override val value: Int) : IntFlags.Bit {
		Always(1 shl 0),
		Once(1 shl 1),
		FirstUseEver(1 shl 2),
		Appearing(1 shl 3)
	}

	fun setNextWindowSize(width: Float, height: Float, cond: Cond = Cond.Always) {
		n.igSetNextWindowSize(Vec2.ByVal(width, height), cond.value)
	}

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

	fun showUserGuide() {
		n.igShowUserGuide()
	}

	fun text(text: String) {
		n.igText(text)
	}

	fun checkbox(label: String, isChecked: Ref<Boolean>): Boolean {
		memstack { mem ->
			val pChecked = isChecked.toBuf(mem)
			return n.igCheckbox(label, pChecked.address)
				.also {
					isChecked.fromBuf(pChecked)
				}
		}
	}

	fun button(label: String, width: Float = 0f, height: Float = 0f): Boolean {
		return n.igButton(label, Vec2.ByVal(width, height))
	}

	fun smallButton(label: String): Boolean {
		return n.igSmallButton(label)
	}

	fun sameLine(pos: Float = 0f, spacing: Float = -1f) {
		n.igSameLine(pos, spacing)
	}

	fun image(
		image: Imgui.ImageDescriptor,
		sizex: Float, sizey: Float,
		uv0x: Float = 0f, uv0y: Float = 0f,
		uv1x: Float = 1f, uv1y: Float = 1f,
		tintr: Float = 1f, tintg: Float = 1f, tintb: Float = 1f, tinta: Float = 1f,
		borderr: Float = 0f, borderg: Float = 0f, borderb: Float = 0f, bordera: Float = 0f
	) {
		n.igImage(
			image.descriptorSet.id,
			Vec2.ByVal(sizex, sizey),
			Vec2.ByVal(uv0x, uv0y),
			Vec2.ByVal(uv1x, uv1y),
			Vec4.ByVal(tintr, tintg, tintb, tinta),
			Vec4.ByVal(borderr, borderg, borderb, bordera)
		)
	}

	fun image(
		image: Imgui.ImageDescriptor,
		size: Extent2D,
		region: Rect2D = Rect2D(Offset2D(0, 0), size),
		tintColor: ColorRGBA = ColorRGBA.Float(1f, 1f, 1f, 1f),
		borderColor: ColorRGBA = ColorRGBA.Float(0f, 0f, 0f, 0f)
	) {
		image(
			image,
			size.width.toFloat(), size.height.toFloat(),
			region.offset.x.toFloat()/image.extent.width.toFloat(),
			region.offset.y.toFloat()/image.extent.height.toFloat(),
			(region.offset.x + region.extent.width).toFloat()/image.extent.width.toFloat(),
			(region.offset.y + region.extent.height).toFloat()/image.extent.height.toFloat(),
			tintColor.rf, tintColor.gf, tintColor.bf, tintColor.af,
			borderColor.rf, borderColor.gf, borderColor.bf, borderColor.af
		)
	}
}
