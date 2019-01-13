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
import java.util.*


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
	InputAttachment;

	class Counts() : EnumMap<DescriptorType,Int>(DescriptorType::class.java) {

		constructor(types: Iterable<DescriptorType>) : this() {
			for (type in types) {
				increment(type)
			}
		}

		constructor(bindings: List<DescriptorSetLayout.Binding>) : this() {
			for (binding in bindings) {
				increment(binding.type)
			}
		}

		constructor(vararg counts: Pair<DescriptorType,Int>) : this() {
			putAll(counts)
		}

		fun increment(type: DescriptorType) {
			compute(type) { _, count -> (count ?: 0) + 1 }
		}

		operator fun times(factor: Int) =
			Counts().apply {
				for ((type, count) in this@Counts) {
					put(type, count*factor)
				}
			}

		companion object {

			fun add(counts: List<Counts>) =
				Counts().apply {
					for (c in counts) {
						for ((type, count) in c) {
							compute(type) { _, totalCount -> (totalCount ?: 0) + count }
						}
					}
				}

			fun add(vararg counts: Counts) = add(counts.toList())
		}
	}
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

	fun allocate(layout: DescriptorSetLayout) =
		allocate(listOf(layout))[0]
}

fun Device.descriptorPool(
	maxSets: Int,
	sizes: DescriptorType.Counts,
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

	inner class Address internal constructor(
		val binding: DescriptorSetLayout.Binding,
		val arrayElement: Int
	) {
		val set: DescriptorSet = this@DescriptorSet

		fun write(
			buffers: List<DescriptorSet.BufferInfo> = emptyList(),
			images: List<DescriptorSet.ImageInfo> = emptyList()
		) = Write(this, buffers, images)

		fun copyTo(other: Address) =
			Copy(this, other)
	}

	fun address(binding: DescriptorSetLayout.Binding, arrayElement: Int = 0) =
		Address(binding, arrayElement)

	data class Write internal constructor(
		val dst: Address,
		val buffers: List<BufferInfo>,
		val images: List<ImageInfo>
	)

	data class Copy internal constructor(
		val src: Address,
		val dst: Address,
		val descriptorCount: Int = 1
	)

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
}

fun Device.updateDescriptorSets(
	writes: List<DescriptorSet.Write> = emptyList(),
	copies: List<DescriptorSet.Copy> = emptyList()
) {
	memstack { mem ->

		val pWrites = VkWriteDescriptorSet.callocStack(writes.size, mem).apply {
			for (w in writes) {
				get()
					.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
					.dstSet(w.dst.set.id)
					.dstBinding(w.dst.binding.binding)
					.dstArrayElement(w.dst.arrayElement)
					.descriptorType(w.dst.binding.type.ordinal)
					.pBufferInfo(
						if (w.buffers.isEmpty()) {
							null
						} else {
							VkDescriptorBufferInfo.callocStack(w.buffers.size, mem).apply {
								for (b in w.buffers) {
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
						if (w.images.isEmpty()) {
							null
						} else {
							VkDescriptorImageInfo.callocStack(w.images.size, mem).apply {
								for (i in w.images) {
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
			}
			flip()
		}

		val pCopies = VkCopyDescriptorSet.callocStack(copies.size, mem).apply {
			for (c in copies) {
				get()
					.sType(VK_STRUCTURE_TYPE_COPY_DESCRIPTOR_SET)
					.srcSet(c.src.set.id)
					.srcBinding(c.src.binding.binding)
					.srcArrayElement(c.src.arrayElement)
					.dstSet(c.dst.set.id)
					.dstBinding(c.dst.binding.binding)
					.dstArrayElement(c.dst.arrayElement)
					.descriptorCount(c.descriptorCount)
			}
			flip()
		}

		vkUpdateDescriptorSets(vkDevice, pWrites, pCopies)
	}
}
