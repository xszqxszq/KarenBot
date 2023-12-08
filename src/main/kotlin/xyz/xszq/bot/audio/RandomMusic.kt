package xyz.xszq.bot.audio

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.flow.filter
import xyz.xszq.nereides.audioExts
import java.io.File

object RandomMusic {
    private val musicDir = localCurrentDirVfs["music"]
    suspend fun get(type: String): VfsFile = musicDir[type].listRecursiveSimple().filter {
        it.extension in audioExts
    }.random()
}