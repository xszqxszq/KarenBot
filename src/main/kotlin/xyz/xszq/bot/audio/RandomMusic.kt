package xyz.xszq.bot.audio

import korlibs.io.file.VfsFile
import korlibs.io.file.extension
import korlibs.io.file.std.localCurrentDirVfs
import xyz.xszq.nereides.audioExts

object RandomMusic {
    private val musicDir = localCurrentDirVfs["music"]
    suspend fun get(type: String): VfsFile = musicDir[type].listRecursiveSimple().filter {
        it.extension in audioExts
    }.random()
}