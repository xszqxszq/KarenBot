package xyz.xszq.bot.audio

import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.extension
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.flow.filter
import xyz.xszq.nereides.audioExts
import java.io.File

object RandomMusic {
    val musicDir = localCurrentDirVfs["music"]
    suspend fun fetchRandom(type: String): VfsFile = musicDir[type].listSimple().filter {
        it.extension in audioExts
    }.random()
}