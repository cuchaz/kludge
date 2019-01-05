/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toBuffer
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class DescriptorSetLayout internal constructor(
	val device: Device,
	internal val id: Long,
	val bindings: List<Binding>
) : AutoCloseable {

	override fun close() {
		vkDestroyDescriptorSetLayout(device.vkDevice, id, null)
	}

	class Binding(
		val binding: Int,
		val type: DescriptorType,
		val stages: IntFlags<ShaderStage>,
		val count: Int = 1
	)
}

fun Device.descriptorSetLayout(
	bindings: List<DescriptorSetLayout.Binding>
): DescriptorSetLayout {
	memstack { mem ->

		val info = VkDescriptorSetLayoutCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
			.flags(0) // reserved for future use
			.pBindings(
				if (bindings.isEmpty()) {
					null
				} else {
					VkDescriptorSetLayoutBinding.callocStack(bindings.size, mem).apply {
						for (b in bindings) {
							get()
								.binding(b.binding)
								.descriptorType(b.type.ordinal)
								.stageFlags(b.stages.value)
								.descriptorCount(b.count)
						}
						flip()
					}
				}
			)

		val pLayout = mem.mallocLong(1)
		vkCreateDescriptorSetLayout(vkDevice, info, null, pLayout)
			.orFail("failed to create descriptor set layout")
		return DescriptorSetLayout(this, pLayout.get(0), bindings)
	}
}


enum class DescriptorType {
	Sampler,
	CombinedImageSampler,
	SampledImage,
	StorageImage,
	UniformTexelBuffer,
	StorageTexelBuffer,
	UniformBuffer,
	StorageBuffer,
	UniformBufferDynamic,
	StorageBufferDynamic,
	InputAttachment
}


class DescriptorPool internal constructor(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		vkDestroyDescriptorPool(device.vkDevice, id, null)
	}

	enum class Create(override val value: Int) : IntFlags.Bit {
		FreeDescriptorSet(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
	}

	fun allocate(layouts: List<DescriptorSetLayout>): List<DescriptorSet> {
		memstack { mem ->

			val info = VkDescriptorSetAllocateInfo.callocStack(mem)
				.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
				.descriptorPool(id)
				.pSetLayouts(layouts.map { it.id }.toBuffer(mem) ?: throw IllegalArgumentException("layouts cannot be empty"))

			val pSets = mem.mallocLong(layouts.size)
			vkAllocateDescriptorSets(device.vkDevice, info, pSets)
				.orFail("failed to allocate descriptor sets")
			return layouts.map { DescriptorSet(device, pSets.get(), this, it) }
		}
	}
}

fun Device.descriptorPool(
	maxSets: Int,
	sizes: Map<DescriptorType,Int>,
	flags: IntFlags<DescriptorPool.Create> = IntFlags(0)
): DescriptorPool {
	memstack { mem ->

		val info = VkDescriptorPoolCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
			.maxSets(maxSets)
			.pPoolSizes(VkDescriptorPoolSize.callocStack(sizes.size, mem).apply {
				for ((type, count) in sizes) {
					get()
						.type(type.ordinal)
						.descriptorCount(count)
				}
				flip()
			})
			.flags(flags.value)

		val pPool = mem.mallocLong(1)
		vkCreateDescriptorPool(vkDevice, info, null, pPool)
		return DescriptorPool(this, pPool.get(0))
	}
}


class DescriptorSet(
	val device: Device,
	internal val id: Long,
	val pool: DescriptorPool,
	val layout: DescriptorSetLayout
) {

	data class BufferInfo(
		val buffer: Buffer,
		val offset: Long = 0L,
		val range: Long = buffer.size
	)

	data class ImageInfo(
		val sampler: Sampler? = null,
		val view: Image.View? = null,
		val layout: Image.Layout? = null
	)

	fun update(
		binding: DescriptorSetLayout.Binding,
		buffers: List<BufferInfo> = emptyList(),
		images: List<ImageInfo> = emptyList(),
		arrayElement: Int = 0
	) {
		memstack { mem ->

			val writes = VkWriteDescriptorSet.callocStack(1, mem).apply {
				get()
					.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
					.dstSet(id)
					.dstBinding(binding.binding)
					.dstArrayElement(arrayElement)
					.descriptorType(binding.type.ordinal)
					.pBufferInfo(
						if (buffers.isEmpty()) {
							null
						} else {
							VkDescriptorBufferInfo.callocStack(buffers.size, mem).apply {
								for (b in buffers) {
									get()
										.buffer(b.buffer.id)
										.offset(b.offset)
										.range(b.range)
								}
								flip()
							}
						}
					)
					.pImageInfo(
						if (images.isEmpty()) {
							null
						} else {
							VkDescriptorImageInfo.callocStack(images.size, mem).apply {
								for (i in images) {
									get()
										.sampler(i.sampler?.id ?: VK_NULL_HANDLE)
										.imageView(i.view?.id ?: VK_NULL_HANDLE)
										.imageLayout(i.layout?.ordinal ?: 0)
								}
								flip()
							}
						}
					)
					.pTexelBufferView(null) // TODO: support texel buffer views?
				flip()
			}

			// TODO: support copies?
			val copies = null

			vkUpdateDescriptorSets(device.vkDevice, writes, copies)
		}
	}
}
