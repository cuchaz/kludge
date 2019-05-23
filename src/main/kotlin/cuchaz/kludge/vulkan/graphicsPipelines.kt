/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toASCII
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK11.*


class GraphicsPipeline internal constructor(
	device: Device,
	id: Long,
	layoutId: Long,
	val vertexInput: VertexInput
) : AutoCloseable, Pipeline(device, PipelineBindPoint.Graphics, id, layoutId)

class VertexInput(block: VertexInput.() -> Unit = {}) {

	val _bindings = ArrayList<Binding>()
	val bindings: List<Binding> get() = _bindings

	init {
		block()
	}

	enum class Rate {
		Vertex,
		Instance
	}

	fun binding(stride: Int, rate: Rate = Rate.Vertex, block: Binding.() -> Unit = {}) =
		Binding(bindings.size, stride, rate).apply {
			_bindings.add(this)
			block()
		}

	val size: Long get() = bindings.map { it.stride.toLong() }.sum()

	inner class Binding internal constructor(
		val index: Int,
		val stride: Int,
		val rate: Rate
	) {

		val vertexInput: VertexInput = this@VertexInput
		val _attributes = ArrayList<Attribute>()
		val attributes: List<Attribute> get() = _attributes

		fun attribute(location: Int, format: Image.Format, offset: Int) =
			Attribute(location, format, offset).apply {
				_attributes.add(this)
			}

		inner class Attribute internal constructor(
			val location: Int,
			val format: Image.Format,
			val offset: Int = 0
		) {
			val binding: Binding = this@Binding
		}
	}
}

data class InputAssembly(
	val topology: Topology,
	val restart: Boolean = false
) {

	enum class Topology {
		PointList,
		LineList,
		LineStrip,
		TriangleList,
		TriangleStrip,
		TriangleFan,
		LineListWithAdjacency,
		LineStripWithAdjacency,
		TriangleListWithAdjacency,
		TriangleStripWithAdjacency,
		PatchList
	}

	companion object {
		const val RestartCode: Int = -1 // ie, 0xffffffff as a signed int
	}
}

data class Viewport(
	val x: Float,
	val y: Float,
	val width: Float,
	val height: Float,
	val minDepth: Float = 0.0f,
	val maxDepth: Float = 1.0f
)
internal fun VkViewport.set(viewport: Viewport) =
	apply {
		x(viewport.x)
		y(viewport.y)
		width(viewport.width)
		height(viewport.height)
		minDepth(viewport.minDepth)
		maxDepth(viewport.maxDepth)
	}
internal fun VkViewport.toViewport() =
	Viewport(
		x(),
		y(),
		width(),
		height(),
		minDepth(),
		maxDepth()
	)

data class RasterizationState(
	val cullMode: IntFlags<CullMode>,
	val frontFace: FrontFace,
	val depthClamp: Boolean = false,
	val discard: Boolean = false,
	val polygonMode: PolygonMode = PolygonMode.Fill,
	val lineWidth: Float = 1.0f,
	val depthBias: DepthBias? = null
)

enum class PolygonMode {
	Fill,
	Line,
	Point
}

enum class CullMode(override val value: Int) : IntFlags.Bit {

	Front(VK_CULL_MODE_FRONT_BIT),
	Back(VK_CULL_MODE_BACK_BIT);

	companion object {
		val None = IntFlags<CullMode>(VK_CULL_MODE_NONE)
		val FrontAndBck = IntFlags<CullMode>(VK_CULL_MODE_FRONT_AND_BACK)
	}
}

enum class FrontFace {
	Counterclockwise,
	Clockwise
}

data class DepthBias(
	val constantFactor: Float,
	val clamp: Float,
	val slopeFactor: Float
)

enum class SampleCount(override val value: Int) : IntFlags.Bit {
	One(VK_SAMPLE_COUNT_1_BIT),
	Two(VK_SAMPLE_COUNT_2_BIT),
	Four(VK_SAMPLE_COUNT_4_BIT),
	Eight(VK_SAMPLE_COUNT_8_BIT),
	Sixteen(VK_SAMPLE_COUNT_16_BIT),
	ThirtyTwo(VK_SAMPLE_COUNT_32_BIT),
	SixtyFour(VK_SAMPLE_COUNT_64_BIT)
}

data class MultisampleState(
	val sampleShading: Boolean = false,
	val rasterizationSamples: SampleCount = SampleCount.One,
	val minSampleShading: Float = 1.0f,
	//val sampleMask: SampleMask // TODO: support this?
	val alphaToCoverage: Boolean = false,
	val alphaToOne: Boolean = false
)

enum class LogicOp {
	Clear,
	And,
	AndReverse,
	Copy,
	AndInverted,
	NoOp,
	Xor,
	Or,
	Nor,
	Equivalent,
	Invert,
	OrReverse,
	CopyInverted,
	OrInverted,
	Nand,
	Set
}

enum class BlendOp {
	Add,
	Subtract,
	ReverseSubtract,
	Min,
	Max
}

enum class BlendFactor {
	Zero,
	One,
	SrcColor,
	OneMinusSrcColor,
	DstColor,
	OneMinusDstColor,
	SrcAlpha,
	OneMinusSrcAlpha,
	DstAlpha,
	DoneMinusDstAlpha,
	ConstantColor,
	OneMinusConstantColor,
	ConstantAlpha,
	OneMinusConstantAlpha,
	SrcAlphaSaturate,
	Src1Color,
	OneMinusSrc1Color,
	Src1Alpha,
	OneMinusSrc1Alpha
}

enum class ColorComponent(override val value: Int) : IntFlags.Bit {
	R(VK_COLOR_COMPONENT_R_BIT),
	G(VK_COLOR_COMPONENT_G_BIT),
	B(VK_COLOR_COMPONENT_B_BIT),
	A(VK_COLOR_COMPONENT_A_BIT)
}

class ColorBlendState(
	val logicOp: LogicOp? = null,
	val blendConstants: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
) {

	data class Attachment(
		val color: Part,
		val alpha: Part,
		val colorWriteMask: IntFlags<ColorComponent> =
			IntFlags.of(
				ColorComponent.R,
				ColorComponent.G,
				ColorComponent.B,
				ColorComponent.A
			)
	) {

		data class Part(
			val src: BlendFactor,
			val dst: BlendFactor,
			val op: BlendOp
		)
	}
}

class DepthStencilState(
	val depthTest: Boolean = true,
	val depthWrite: Boolean = true,
	val depthCompareOp: CompareOp = CompareOp.Less,
	val depthBoundsTest: Boolean = false,
	val stencilTest: Boolean = false,
	val stencilFront: StencilOpState? = null,
	val stencilBack: StencilOpState? = null,
	val minDepth: Float = 0f,
	val maxDepth: Float = 1f
)

class StencilOpState(
	val failOp: StencilOp,
	val passOp: StencilOp,
	val depthFailOp: StencilOp,
	val compareOp: StencilOp,
	val compareMask: Int,
	val writeMask: Int,
	val reference: Int
) {

	internal fun toVulkan(mem: MemoryStack) =
		VkStencilOpState.callocStack(mem)
			.failOp(failOp.ordinal)
			.passOp(passOp.ordinal)
			.depthFailOp(depthFailOp.ordinal)
			.compareOp(compareOp.ordinal)
			.compareMask(compareMask)
			.writeMask(writeMask)
			.reference(reference)
}

enum class StencilOp {
	Keep,
	Zero,
	Replace,
	IncrementAndClamp,
	DecrementAndClamp,
	Invert,
	IncrementAndWrap,
	DecrementAndWrap
}


enum class PipelineBindPoint {
	Graphics,
	Compute
}

data class PushConstantRange(
	val stages: IntFlags<ShaderStage>,
	val size: Int,
	val offset: Int = 0
)

fun VkPushConstantRange.set(src: PushConstantRange) {
	stageFlags(src.stages.value)
	offset(src.offset)
	size(src.size)
}


fun Device.graphicsPipeline(
	renderPass: RenderPass,
	stages: List<ShaderModule.Stage>,
	descriptorSetLayouts: List<DescriptorSetLayout> = emptyList(),
	pushConstantRanges: List<PushConstantRange> = emptyList(),
	vertexInput: VertexInput = VertexInput(),
	inputAssembly: InputAssembly,
	rasterizationState: RasterizationState,
	viewports: List<Viewport>,
	scissors: List<Rect2D>,
	multisampleState: MultisampleState = MultisampleState(),
	colorBlend: ColorBlendState = ColorBlendState(),
	colorAttachmentBlends: Map<Attachment,ColorBlendState.Attachment?>,
	depthStencilState: DepthStencilState? = null
): GraphicsPipeline {
	memstack { mem ->

		// build stage info
		val pStage = VkPipelineShaderStageCreateInfo.callocStack(stages.size, mem)
		for (stage in stages) {
			pStage.get()
				.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
				.stage(stage.stage.value)
				.module(stage.module.id)
				.pName(stage.name.toASCII(mem))
				//.pSpecializationInfo(null) // TODO: support this?
		}
		pStage.flip()

		// build vertex input state
		val pVertexInput = VkPipelineVertexInputStateCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
			.pVertexBindingDescriptions(
				if (vertexInput.bindings.isEmpty()) {
					null
				} else {
					VkVertexInputBindingDescription.callocStack(vertexInput.bindings.size, mem).apply {
						for (b in vertexInput.bindings) {
							get()
								.binding(b.index)
								.stride(b.stride)
								.inputRate(b.rate.ordinal)
						}
						flip()
					}
				}
			)
			.pVertexAttributeDescriptions(run {
				val attrs = vertexInput.bindings.flatMap { it.attributes }
				if (attrs.isEmpty()) {
					null
				} else {
					VkVertexInputAttributeDescription.callocStack(attrs.size, mem).apply {
						for (a in attrs) {
							get()
								.binding(a.binding.index)
								.location(a.location)
								.format(a.format.ordinal)
								.offset(a.offset)
						}
						flip()
					}
				}
			})

		// build input assembly state
		val pInputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
			.topology(inputAssembly.topology.ordinal)
			.primitiveRestartEnable(inputAssembly.restart)

		// build the viewports and scissors
		val pViewports = VkViewport.callocStack(viewports.size, mem)
		for (viewport in viewports) {
			pViewports.get().set(viewport)
		}
		pViewports.flip()
		val pScissors = VkRect2D.callocStack(scissors.size, mem)
		for (scissor in scissors) {
			pScissors.get().set(scissor)
		}
		pScissors.flip()
		val pViewport = VkPipelineViewportStateCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
			.pViewports(pViewports)
			.pScissors(pScissors)

		// build the rasterization state
		val pRaster = VkPipelineRasterizationStateCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
			.depthClampEnable(rasterizationState.depthClamp)
			.rasterizerDiscardEnable(rasterizationState.discard)
			.polygonMode(rasterizationState.polygonMode.ordinal)
			.lineWidth(rasterizationState.lineWidth)
			.cullMode(rasterizationState.cullMode.value)
			.frontFace(rasterizationState.frontFace.ordinal)
			.depthClampEnable(rasterizationState.depthBias != null)
		rasterizationState.depthBias?.let {
			pRaster.depthBiasConstantFactor(it.constantFactor)
			pRaster.depthBiasClamp(it.clamp)
			pRaster.depthBiasSlopeFactor(it.slopeFactor)
		}

		// build multisample state
		val pMultisample = VkPipelineMultisampleStateCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
			.sampleShadingEnable(multisampleState.sampleShading)
			.rasterizationSamples(multisampleState.rasterizationSamples.value)
			.minSampleShading(multisampleState.minSampleShading)
			//.pSampleMask(IntBuffer) // TODO: support this?
			.alphaToCoverageEnable(multisampleState.alphaToCoverage)
			.alphaToOneEnable(multisampleState.alphaToOne)

		// build the pipeline layout
		val pLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
			.pSetLayouts(
				if (descriptorSetLayouts.isEmpty()) {
					null
				} else {
					descriptorSetLayouts.map { it.id }.toBuffer(mem)
				}
			)
			.pPushConstantRanges(
				if (pushConstantRanges.isEmpty()) {
					null
				} else {
					VkPushConstantRange.callocStack(pushConstantRanges.size, mem).apply {
						for (range in pushConstantRanges) {
							get().set(range)
						}
						flip()
					}
				}
			)
		val pLayout = mem.mallocLong(1)
		vkCreatePipelineLayout(vkDevice, pLayoutInfo, null, pLayout)
			.orFail("failed to create pipeline layout")

		// build color blend state
		val pBlendAttachments = VkPipelineColorBlendAttachmentState.callocStack(colorAttachmentBlends.size, mem)
		for (attachment in renderPass.attachments) {

			// skip an attachment when there's no color blend for it
			if (!colorAttachmentBlends.containsKey(attachment)) {
				continue
			}

			val blend = colorAttachmentBlends[attachment]
			pBlendAttachments.get().apply {
				if (blend != null) {
					blendEnable(true)
					srcColorBlendFactor(blend.color.src.ordinal)
					dstColorBlendFactor(blend.color.dst.ordinal)
					colorBlendOp(blend.color.op.ordinal)
					srcAlphaBlendFactor(blend.alpha.src.ordinal)
					dstAlphaBlendFactor(blend.alpha.dst.ordinal)
					alphaBlendOp(blend.alpha.op.ordinal)
					colorWriteMask(blend.colorWriteMask.value)
				} else {
					blendEnable(false)
				}
			}
		}
		pBlendAttachments.flip()
		val pColorBlend = VkPipelineColorBlendStateCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
			.logicOpEnable(colorBlend.logicOp != null)
			.logicOp(colorBlend.logicOp?.ordinal ?: 0)
			.pAttachments(pBlendAttachments)
			.blendConstants(0, colorBlend.blendConstants[0])
			.blendConstants(1, colorBlend.blendConstants[1])
			.blendConstants(2, colorBlend.blendConstants[2])
			.blendConstants(3, colorBlend.blendConstants[3])

		// build the depth/stencil state, if needed
		val pDepthStencilState = depthStencilState?.let {
			VkPipelineDepthStencilStateCreateInfo.callocStack(mem)
				.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
				.flags(0)
				.depthTestEnable(it.depthTest)
				.depthWriteEnable(it.depthWrite)
				.depthCompareOp(it.depthCompareOp.ordinal)
				.depthBoundsTestEnable(it.depthBoundsTest)
				.stencilTestEnable(it.stencilTest)
				.front(it.stencilFront?.toVulkan(mem) ?: VkStencilOpState.callocStack(mem))
				.back(it.stencilBack?.toVulkan(mem) ?: VkStencilOpState.callocStack(mem))
				.minDepthBounds(it.minDepth)
				.maxDepthBounds(it.maxDepth)
		}

		// TODO: do we need any of these?
		//pTessellationState(@Nullable @NativeType("VkPipelineTessellationStateCreateInfo const *") VkPipelineTessellationStateCreateInfo value) { npTessellationState(address(), value); return this; }

		// finally! build the graphics pipeline
		val info = VkGraphicsPipelineCreateInfo.callocStack(1, mem)
		info.get()
			.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
			.pStages(pStage)
			.pVertexInputState(pVertexInput)
			.pInputAssemblyState(pInputAssembly)
			.pViewportState(pViewport)
			.pRasterizationState(pRaster)
			.pMultisampleState(pMultisample)
			.pDynamicState(null) // TODO: support dynamic state?
			.layout(pLayout.get(0))
			.renderPass(renderPass.id)
			.pColorBlendState(pColorBlend)
			.pDepthStencilState(pDepthStencilState)
			.subpass(0) // TODO: support other subpasses?
			.basePipelineHandle(VK_NULL_HANDLE) // TODO: support derivative pipelines?
			.basePipelineIndex(-1)
		info.flip()

		val pPipeline = mem.mallocLong(1)
		vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, info, null, pPipeline)
			.orFail("failed to create graphics pipeline")
		return GraphicsPipeline(this, pPipeline.get(0), pLayout.get(0), vertexInput)
	}
}
