/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.memstack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*


class Sampler(
	val device: Device,
	internal val id: Long
) : AutoCloseable {


	enum class Filter {
		Nearest,
		Linear
	}

	enum class Mipmap {
		Nearest,
		Linear
	}

	enum class Address {
		Repeat,
		MirroredRepeat,
		ClampToEdge,
		ClampToBorder,
		MirrorClampToEdge
	}

	enum class BorderColor {
		FloatTransparentBlack,
		IntTransparentBlack,
		FloatOpaqueBlack,
		IntOpaqueBlack,
		FloatOpaqueWhite,
		IntOpaqueWhite
	}

	override fun close() {
		vkDestroySampler(device.vkDevice, id, null)
	}
}

fun Device.sampler(
	magFilter: Sampler.Filter = Sampler.Filter.Nearest,
	minFilter: Sampler.Filter = Sampler.Filter.Nearest,
	mipmap: Sampler.Mipmap = Sampler.Mipmap.Nearest,
	addressU: Sampler.Address = Sampler.Address.Repeat,
	addressV: Sampler.Address = Sampler.Address.Repeat,
	addressW: Sampler.Address = Sampler.Address.Repeat,
	mipLodBias: Float = 0.0f,
	maxAnisotropy: Float? = null,
	compareOp: CompareOp? = null,
	borderColor: Sampler.BorderColor = Sampler.BorderColor.IntOpaqueBlack,
	minLod: Float = 0.0f,
	maxLod: Float = 0.0f,
	normalizedCoordinates: Boolean = true
): Sampler {
	memstack { mem ->

		val info = VkSamplerCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
			.flags(0) // reserved for future use
			.magFilter(magFilter.ordinal)
			.minFilter(minFilter.ordinal)
			.mipmapMode(mipmap.ordinal)
			.addressModeU(addressU.ordinal)
			.addressModeV(addressV.ordinal)
			.addressModeW(addressW.ordinal)
			.mipLodBias(mipLodBias)
			.anisotropyEnable(maxAnisotropy != null)
			.maxAnisotropy(maxAnisotropy ?: 0.0f)
			.compareEnable(compareOp != null)
			.compareOp(compareOp?.ordinal ?: 0)
			.minLod(minLod)
			.maxLod(maxLod)
			.borderColor(borderColor.ordinal)
			.unnormalizedCoordinates(!normalizedCoordinates)

		val pSampler = mem.mallocLong(1)
		vkCreateSampler(vkDevice, info, null, pSampler)
			.orFail("failed to create sampler")
		return Sampler(this, pSampler.get(0))
	}
}


enum class CompareOp {
	Never,
	Less,
	Equal,
	LessOrEqual,
	Greater,
	NotEqual,
	GreaterOrEqual,
	Always
}
