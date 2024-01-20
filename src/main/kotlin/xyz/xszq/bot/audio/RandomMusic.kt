package xyz.xszq.bot.audio

import korlibs.io.file.VfsFile
import korlibs.io.file.extensionLC
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import xyz.xszq.nereides.audioExts

object RandomMusic {
    private val musicDir = localCurrentDirVfs["music"]
    suspend fun get(type: String): VfsFile = musicDir[type].listRecursive().mapNotNull {
        if (it.isDirectory()) {
            it.list().filter { f -> f.extensionLC in audioExts }.toList().random()
        } else if (it.extensionLC in audioExts) {
            it
        } else {
            null
        }
    }.toList().random()
}