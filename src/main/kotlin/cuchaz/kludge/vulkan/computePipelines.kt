/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toASCII
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class ComputePipeline internal constructor(
	device: Device,
	id: Long,
	layoutId: Long
) : AutoCloseable, Pipeline(device, PipelineBindPoint.Compute, id, layoutId)

fun Device.computePipeline(
	stage: ShaderModule.Stage,
	descriptorSetLayouts: List<DescriptorSetLayout> = emptyList()
): ComputePipeline {
	memstack { mem ->

		// build stage info
		val pStage = VkPipelineShaderStageCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
			.stage(stage.stage.value)
			.module(stage.module.id)
			.pName(stage.name.toASCII(mem))
			.pSpecializationInfo(null) // TODO: support this?

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
			.pPushConstantRanges(null) // TODO: support constant ranges?
		val pLayout = mem.mallocLong(1)
		vkCreatePipelineLayout(vkDevice, pLayoutInfo, null, pLayout)
			.orFail("failied to create pipeline layout")

		// build the compute pipeline
		val info = VkComputePipelineCreateInfo.callocStack(1, mem)
		info.get()
			.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
			.stage(pStage)
			.layout(pLayout.get(0))
			.basePipelineHandle(VK_NULL_HANDLE) // TODO: support derivative pipelines?
			.basePipelineIndex(-1)
		info.flip()

		val pPipeline = mem.mallocLong(1)
		vkCreateComputePipelines(vkDevice, VK_NULL_HANDLE, info, null, pPipeline)
			.orFail("failed to create compute pipeline")
		return ComputePipeline(this, pPipeline.get(0), pLayout.get(0))
	}
}
