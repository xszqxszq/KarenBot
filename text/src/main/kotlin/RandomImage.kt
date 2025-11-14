package xyz.xszq.bot

import korlibs.datastructure.random.fastRandom
import korlibs.io.file.extensionLC
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class RandomImage {
    @OptIn(ExperimentalCoroutinesApi::class)
    val files = runBlocking {
        localCurrentDirVfs[DIR].list().filter { it.isDirectory() }.flatMapConcat {
            it.listRecursive {
                it.extensionLC in IMAGE_EXTS
            }
        }.toList()
    }
    fun random() = files.fastRandom()
    companion object {
        const val DIR = "data/random/kinpatsu"
        val IMAGE_EXTS = listOf("jpg", "jpeg", "png", "gif", "webp")
    }
}