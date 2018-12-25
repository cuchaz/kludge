package cuchaz.kludge.vulkan

import cuchaz.kludge.tools.IntFlags
import cuchaz.kludge.tools.memstack
import cuchaz.kludge.tools.toByteBuffer
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path


class ShaderModule(
	val device: Device,
	internal val id: Long
) : AutoCloseable {

	override fun close() {
		vkDestroyShaderModule(device.vkDevice, id, null)
	}

	inner class Stage(
		val name: String,
		val stage: IntFlags<ShaderStage>
	) {
		val module: ShaderModule = this@ShaderModule
	}
}


fun Device.shaderModule(code: ByteBuffer): ShaderModule {
	memstack { mem ->

		val info = VkShaderModuleCreateInfo.callocStack(mem)
			.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
			.pCode(code)

		val pMod = mem.mallocLong(1)
		vkCreateShaderModule(vkDevice, info, null, pMod)
		return ShaderModule(this, pMod.get(0))
	}
}

fun Device.shaderModule(file: File) = shaderModule(file.toByteBuffer())
fun Device.shaderModule(path: Path) = shaderModule(path.toByteBuffer())


enum class ShaderStage(override val value: Int) : IntFlags.Bit {

	Vertex(VK_SHADER_STAGE_VERTEX_BIT),
	TessellationControl(VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT),
	TessellationEvaluation(VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT),
	Geometry(VK_SHADER_STAGE_GEOMETRY_BIT),
	Fragment(VK_SHADER_STAGE_FRAGMENT_BIT),
	Compute(VK_SHADER_STAGE_COMPUTE_BIT);

	companion object {
		val AllGraphics = IntFlags<ShaderStage>(VK_SHADER_STAGE_ALL_GRAPHICS)
		val All = IntFlags<ShaderStage>(VK_SHADER_STAGE_ALL)
	}
}