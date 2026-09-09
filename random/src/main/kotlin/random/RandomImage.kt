package xyz.xszq.bot.random

import korlibs.datastructure.random.fastRandom
import korlibs.io.file.VfsFile
import korlibs.io.file.extensionLC
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * 随机图片
 */
class RandomImage {
    @OptIn(ExperimentalCoroutinesApi::class)
    lateinit var files: List<VfsFile>

    fun random() = files.fastRandom()

    companion object {
        const val DIR = "data/random/kinpatsu"
        val IMAGE_EXTS = listOf("jpg", "jpeg", "png", "gif", "webp")
    }

    /**
     * 初始化载入图片
     */
    suspend fun init() {
        files = localCurrentDirVfs[DIR].listRecursiveSimple().filter { file ->
            file.extensionLC in IMAGE_EXTS
        }.toList()
    }
}