/*
 * Copyright (c) 2019, Cuchaz Interactive, LLC. All rights reserved.
 * License terms are at license.txt in the project root
 */

package cuchaz.kludge.window

import cuchaz.kludge.tools.memstack
import org.lwjgl.util.nfd.NFDPathSet
import org.lwjgl.util.nfd.NativeFileDialog.*
import java.nio.file.Path
import java.nio.file.Paths


class FilterList(vararg val filters: List<String>) {

	internal val str: String? by lazy {
		if (filters.isEmpty()) {
			null
		} else {
			filters.joinToString(";") { it.joinToString(",") }
		}
	}
}

object FileDialog {

	fun openFile(filterList: FilterList? = null, defaultPath: Path? = null): Path? {
		memstack { mem ->
			val outPath = mem.mallocPointer(1)
			val result = NFD_OpenDialog(filterList?.str, defaultPath?.toString(), outPath)
			when (result) {
				NFD_OKAY -> {
					val str = outPath.getStringUTF8(0)
					nNFD_Free(outPath.get(0))
					return Paths.get(str)
				}
				NFD_CANCEL -> return null
				else -> throw FileDialogException("can't open dialog: open file")
			}
		}
	}

	fun openFiles(filterList: FilterList? = null, defaultPath: Path? = null): List<Path>? {
		memstack { mem ->
			val pathSet = NFDPathSet.mallocStack(mem)
			val result = NFD_OpenDialogMultiple(filterList?.str, defaultPath?.toString(), pathSet)
			when (result) {
				NFD_OKAY -> {
					val count = NFD_PathSet_GetCount(pathSet)
					val paths = (0 until count).map { Paths.get(NFD_PathSet_GetPath(pathSet, it)!!) }
					NFD_PathSet_Free(pathSet)
					return paths
				}
				NFD_CANCEL -> return null
				else -> throw FileDialogException("can't open dialog: open files")
			}
		}
	}

	fun saveFile(filterList: FilterList? = null, defaultPath: Path? = null): Path? {
		memstack { mem ->
			val outPath = mem.mallocPointer(1)
			val result = NFD_SaveDialog(filterList?.str, defaultPath?.toString(), outPath)
			when (result) {
				NFD_OKAY -> {
					val str = outPath.getStringUTF8(0)
					nNFD_Free(outPath.get(0))
					return Paths.get(str)
				}
				NFD_CANCEL -> return null
				else -> throw FileDialogException("can't open dialog: save file")
			}
		}
	}

	fun pickFolder(defaultPath: Path? = null): Path? {
		memstack { mem ->
			val outPath = mem.mallocPointer(1)
			val result = NFD_PickFolder(defaultPath?.toString(), outPath)
			when (result) {
				NFD_OKAY -> {
					val str = outPath.getStringUTF8(0)
					nNFD_Free(outPath.get(0))
					return Paths.get(str)
				}
				NFD_CANCEL -> return null
				else -> throw FileDialogException("can't open dialog: pick folder")
			}
		}
	}
}

class FileDialogException(msg: String) : RuntimeException("$msg: ${NFD_GetError()}")
