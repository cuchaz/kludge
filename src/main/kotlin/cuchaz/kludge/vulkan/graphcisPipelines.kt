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
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.*


class GraphicsPipeline(
	val device: Device,
	internal val layoutId: Long,
	internal val renderPassId: Long,
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		vkDestroyPipelineLayout(device.vkDevice, layoutId, null)
		vkDestroyRenderPass(device.vkDevice, renderPassId, null)
		vkDestroyPipeline(device.vkDevice, id, null)
	}
}

data class VertexInput(
	val bindingCount: Int = 0,
	val attributesCount: Int = 0
	// TODO: bindings and descriptions?
)

data class InputAssembly(
	val topology: Topology,
	val restart: Boolean
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
) {
	internal fun toVulkan(mem: MemoryStack) = VkViewport.callocStack(mem).set(this)
}
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
	val depthClamp: Boolean = false,
	val discard: Boolean = false,
	val polygonMode: PolygonMode = PolygonMode.Fill,
	val lineWidth: Float = 1.0f,
	val cullMode: IntFlags<CullMode> = IntFlags.of(CullMode.Back),
	val frontFace: FrontFace = FrontFace.Counterclockwise,
	val depthBias: DepthBias? = null
) {

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
}

enum class SampleCount(override val value: Int) : IntFlags.Bit {
	Bits1(VK_SAMPLE_COUNT_1_BIT),
	Bits2(VK_SAMPLE_COUNT_2_BIT),
	Bits4(VK_SAMPLE_COUNT_4_BIT),
	Bits8(VK_SAMPLE_COUNT_8_BIT),
	Bits16(VK_SAMPLE_COUNT_16_BIT),
	Bits32(VK_SAMPLE_COUNT_32_BIT),
	Bits64(VK_SAMPLE_COUNT_64_BIT)
}

data class MultisampleState(
	val sampleShading: Boolean = false,
	val rasterizationSamples: SampleCount = SampleCount.Bits1,
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

// TODO: related to framebuffer list... use that info directly somehow?
class ColorBlendState(
	val attachments: List<Attachment?>, // TODO: should this be a list?
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
			val srcBlendFactor: BlendFactor,
			val dstBlendFactor: BlendFactor,
			val blendOp: BlendOp
		)
	}
}

enum class ImageLayout(val value: Int) {
	Undefined(VK_IMAGE_LAYOUT_UNDEFINED),
	General(VK_IMAGE_LAYOUT_GENERAL),
	ColorAttachmentOptimal(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL),
	DepthStencilAttachmentOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL),
	DepthStencilReadOnlyOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL),
	ShaderReadOnlyOptimal(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
	TransferSrcOptimal(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL),
	TransferDstOptimal(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL),
	Preinitialized(VK_IMAGE_LAYOUT_PREINITIALIZED),
	PresentSrc(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
}

data class AttachmentDescription(
	val format: Format,
	val samples: SampleCount = SampleCount.Bits1,
	val loadOp: LoadOp = LoadOp.DontCare,
	val storeOp: StoreOp = StoreOp.DontCare,
	val stencilLoadOp: LoadOp = LoadOp.DontCare,
	val stencilStoreOp: StoreOp = StoreOp.DontCare,
	val initialLayout: ImageLayout = ImageLayout.Undefined,
	val finalLayout: ImageLayout

) {
	enum class LoadOp {
		Load,
		Clear,
		DontCare
	}

	enum class StoreOp {
		Store,
		DontCare
	}
}

data class AttachmentReference(
	val attachmentIndex: Int, // TODO: hide this index somehow?
	val layout: ImageLayout
) {
	internal fun toVulkan(mem: MemoryStack) = VkAttachmentReference.callocStack(mem).set(this)
}
internal fun VkAttachmentReference.set(ref: AttachmentReference) =
	apply {
		attachment(ref.attachmentIndex)
		layout(ref.layout.ordinal)
	}
internal fun Collection<AttachmentReference>.toBuffer(mem: MemoryStack) =
	if (isEmpty()) {
		null
	} else {
		VkAttachmentReference.callocStack(size, mem).apply {
			for (ref in this@toBuffer) {
				get().set(ref)
			}
			flip()
		}
	}

data class Subpass(
	val pipelineBindPoint: PipelineBindPoint,
	val inputAttachments: List<AttachmentReference> = emptyList(),
	val colorAttachments: List<AttachmentReference> = emptyList(),
	val resolveAttachments: List<AttachmentReference> = emptyList(),
	val depthStencilAttachment: AttachmentReference? = null,
	val preserveAttachments: List<Int> = emptyList() // TODO: hide these indices somehow?
) {

	enum class PipelineBindPoint {
		Graphics,
		Compute
	}
}

enum class AccessFlags(override val value: Int) : IntFlags.Bit {
	IndirectCommandRead(VK_ACCESS_INDIRECT_COMMAND_READ_BIT),
	IndexRead(VK_ACCESS_INDEX_READ_BIT),
	VertexAttributeRead(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT),
	UniformRead(VK_ACCESS_UNIFORM_READ_BIT),
	InputAttachmentRead(VK_ACCESS_INPUT_ATTACHMENT_READ_BIT),
	ShaderRead(VK_ACCESS_SHADER_READ_BIT),
	ShaderWrite(VK_ACCESS_SHADER_WRITE_BIT),
	ColorAttachmentRead(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT),
	ColorAttachmentWrite(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT),
	DepthStencilAttachmentRead(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT),
	DepthStencilAttachmentWrite(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT),
	TransferRead(VK_ACCESS_TRANSFER_READ_BIT),
	TransferWrite(VK_ACCESS_TRANSFER_WRITE_BIT),
	HostRead(VK_ACCESS_HOST_READ_BIT),
	HostWrite(VK_ACCESS_HOST_WRITE_BIT),
	MemoryRead(VK_ACCESS_MEMORY_READ_BIT),
	MemoryWrite(VK_ACCESS_MEMORY_WRITE_BIT)
}

enum class DependencyFlags(override val value: Int) : IntFlags.Bit {
	ByRegion(VK_DEPENDENCY_BY_REGION_BIT)
}

data class SubpassDependency(
	val srcSubpass: Int,
	val dstSubpass: Int,
	val srcStageMask: IntFlags<PipelineStage>,
	val dstStageMask: IntFlags<PipelineStage>,
	val srcAccessMask: IntFlags<AccessFlags>,
	val dstAccessMask: IntFlags<AccessFlags>,
	val dependencyFlags: IntFlags<DependencyFlags> = IntFlags(0)
) {

	companion object {
		const val External = VK_SUBPASS_EXTERNAL
	}
}


fun Device.graphicsPipeline(
	stages: List<ShaderModule.Stage>,
	vertexInput: VertexInput, // TODO: use this
	inputAssembly: InputAssembly,
	viewports: List<Viewport>,
	scissors: List<Rect2D>,
	rasterizationState: RasterizationState = RasterizationState(),
	multisampleState: MultisampleState = MultisampleState(),
	attachments: List<AttachmentDescription>, // TODO: find a way to cement the attachment order and indices/references
	colorBlend: ColorBlendState,
	subpasses: List<Subpass>,
	subpassDependencies: List<SubpassDependency>
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
			.pVertexBindingDescriptions(null) // TODO: support vertex data description
			.pVertexAttributeDescriptions(null)

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
			.pSetLayouts(null) // TODO: support layouts?
			.pPushConstantRanges(null) // TODO: support constant ranges?
		val pLayout = mem.mallocLong(1)
		vkCreatePipelineLayout(vkDevice, pLayoutInfo, null, pLayout)
			.orFail("failied to create pipeline layout")

		// build the render pass
		val pAttachments = VkAttachmentDescription.callocStack(attachments.size, mem)
		for (attachment in attachments) {
			pAttachments.get()
				.format(attachment.format.ordinal)
				.samples(attachment.samples.value)
				.loadOp(attachment.loadOp.ordinal)
				.storeOp(attachment.storeOp.ordinal)
				.stencilLoadOp(attachment.stencilLoadOp.ordinal)
				.stencilStoreOp(attachment.stencilStoreOp.ordinal)
				.initialLayout(attachment.initialLayout.value)
				.finalLayout(attachment.finalLayout.value)
		}
		pAttachments.flip()
		val pSubpasses = VkSubpassDescription.callocStack(subpasses.size, mem)
		for (subpass in subpasses) {
			pSubpasses.get()
				.pipelineBindPoint(subpass.pipelineBindPoint.ordinal)
				.pInputAttachments(subpass.inputAttachments.toBuffer(mem))
				.pColorAttachments(subpass.colorAttachments.toBuffer(mem))
				.colorAttachmentCount(subpass.colorAttachments.size)
				.pResolveAttachments(subpass.resolveAttachments.toBuffer(mem))
				.pDepthStencilAttachment(subpass.depthStencilAttachment?.toVulkan(mem))
				.pPreserveAttachments(subpass.preserveAttachments.toBuffer(mem))
		}
		pSubpasses.flip()
		val pSubpassDependencies = VkSubpassDependency.callocStack(subpassDependencies.size, mem)
		for (subpassDependency in subpassDependencies) {
			pSubpassDependencies.get()
				.srcSubpass(subpassDependency.srcSubpass)
				.dstSubpass(subpassDependency.dstSubpass)
				.srcStageMask(subpassDependency.srcStageMask.value)
				.dstStageMask(subpassDependency.dstStageMask.value)
				.srcAccessMask(subpassDependency.srcAccessMask.value)
				.dstAccessMask(subpassDependency.dstAccessMask.value)
				.dependencyFlags(subpassDependency.dependencyFlags.value)
		}
		pSubpassDependencies.flip()
		val pRenderPassInfo = VkRenderPassCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
			.pAttachments(pAttachments)
			.pSubpasses(pSubpasses)
			.pDependencies(pSubpassDependencies)
		val pRenderPass = mem.mallocLong(1)
		vkCreateRenderPass(vkDevice, pRenderPassInfo, null, pRenderPass)
			.orFail("failed to create render pass")

		// TEMP
		assert (attachments.size == colorBlend.attachments.size)

		// build color blend state
		val pBlendAttachments = VkPipelineColorBlendAttachmentState.callocStack(colorBlend.attachments.size, mem)
		for (attachment in colorBlend.attachments) {
			pBlendAttachments.get().apply {
				if (attachment != null) {
					blendEnable(true)
					srcColorBlendFactor(attachment.color.srcBlendFactor.ordinal)
					dstColorBlendFactor(attachment.color.dstBlendFactor.ordinal)
					colorBlendOp(attachment.color.blendOp.ordinal)
					srcAlphaBlendFactor(attachment.alpha.srcBlendFactor.ordinal)
					dstAlphaBlendFactor(attachment.alpha.dstBlendFactor.ordinal)
					alphaBlendOp(attachment.alpha.blendOp.ordinal)
					colorWriteMask(attachment.colorWriteMask.value)
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

		// TODO: depth and stencil testing
		//pDepthStencilState(@Nullable @NativeType("VkPipelineDepthStencilStateCreateInfo const *") VkPipelineDepthStencilStateCreateInfo value) { npDepthStencilState(address(), value); return this; }

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
			.renderPass(pRenderPass.get(0))
			.pColorBlendState(pColorBlend)
			.subpass(0) // TODO: support other subpasses?
			.basePipelineHandle(VK_NULL_HANDLE) // TODO: support derivative pipelines?
			.basePipelineIndex(-1)
		info.flip()

		val pPipeline = mem.mallocLong(1)
		vkCreateGraphicsPipelines(vkDevice, VK_NULL_HANDLE, info, null, pPipeline)
			.orFail("failed to create graphics pipeline")
		return GraphicsPipeline(this, pLayout.get(0), pRenderPass.get(0), pPipeline.get(0))
	}
}
