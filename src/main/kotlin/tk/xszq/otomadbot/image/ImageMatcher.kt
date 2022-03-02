@file:Suppress("unused")

package tk.xszq.otomadbot.image

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import dev.brachtendorf.jimagehash.hash.Hash
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import kotlinx.coroutines.Dispatchers
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.getMIMEType
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

suspend fun VfsFile.isValidGIF(): Boolean {
    return try {
        val header = readRangeBytes(0..5)
        header.contentEquals("GIF89a".toByteArray()) || header.contentEquals("GIF87a".toByteArray())
    } catch (e: Exception) {
        false
    }
}
fun isImage(filename: Path): Boolean {
    return getMIMEType(filename).split("/")[0] == "image"
}

object ImageMatcher {
    var hash = mutableMapOf<String, MutableList<Hash>>()
    private val hasher = DifferenceHash(64, DifferenceHash.Precision.Triple)

    suspend fun loadImages(type: String, target: String = type) {
        if (!hash.containsKey(target))
            hash[target] = mutableListOf()
        OtomadBotCore.configFolder.resolve("image/$type").toVfs().listRecursive().collect {
            launchImmediately(Dispatchers.IO) {
               if (it.isFile() && isImage(Paths.get(it.absolutePath)) && !it.isValidGIF()) {
                    hash[target]!!.add(hasher.hash(File(it.absolutePath)))
                }
            }
        }
    }
    fun clearImages(target: String) {
        hash[target] = mutableListOf()
    }
    fun matchImage(type: String, target: File): Boolean {
        val now = hasher.hash(target)
        hash[type]!!.fastForEach {
            if (now.normalizedHammingDistanceFast(it) < .2 )
                return true
        }
        return false
    }
}