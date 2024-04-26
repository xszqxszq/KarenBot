package xyz.xszq.karenbot.image

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import xyz.xszq.KarenBot
import xyz.xszq.karenbot.kotlin.getMIMEType
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths


suspend fun VfsFile.isValidGIF(): Boolean = withContext(Dispatchers.IO) {
    try {
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
    private var hash = mutableMapOf<String, MutableList<BigInteger>>()

    suspend fun loadImages(type: String, target: String = type) {
        if (!hash.containsKey(target))
            hash[target] = mutableListOf()
        coroutineScope {
            KarenBot.configFolder.resolve("image/$type").toVfs().listRecursive().collect {
                launchImmediately {
                    if (it.isFile() && isImage(Paths.get(it.absolutePath)) && !it.isValidGIF()) {
                        hash[target]!!.add(differenceHashTriple.calc(File(it.absolutePath)
                            .readAsImage().toBMP32()))
                    }
                }
            }
        }
    }
    fun clearImages(target: String) {
        hash[target] = mutableListOf()
    }
    suspend fun matchImage(type: String, target: File): Boolean {
        val now = differenceHashTriple.calc(target.readAsImage().toBMP32())
        hash[type]!!.fastForEach {
            if (differenceHashTriple.similarity(now, it) > .92 )
                return true
        }
        return false
    }
}