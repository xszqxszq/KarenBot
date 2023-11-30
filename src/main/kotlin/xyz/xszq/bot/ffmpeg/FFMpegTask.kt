@file:Suppress("unused")

package xyz.xszq.bot.ffmpeg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.xszq.nereides.newTempFile
import java.io.File

class FFMpegTask(
    private val outputFormat: FFMpegFileType,
    private val argsBuilder: Builder.() -> Unit
) {
    @DslMarker annotation class FFMpegBuilder
    @FFMpegBuilder
    inner class Builder {
        val arguments = mutableListOf<Argument>()
        private fun insert(arg: Argument) = arguments.add(arg)
        fun input(path: String) = insert(Argument("i", path))
        fun input(path: File) = insert(Argument("i", path.absolutePath))
        fun startAt(timeInSecond: Double) = insert(Argument("ss", timeInSecond.toString()))
        fun duration(timeInSecond: Double) = insert(Argument("t", timeInSecond.toString()))
        fun audioRate(rate: String) = insert(Argument("ar", rate))
        fun audioChannels(channels: Int) = insert(Argument("ac", channels.toString()))
        fun yes() = insert(Argument("y"))
        fun audioFilter(filter: String) = insert(Argument("af", filter))
        fun acodec(type: String) = insert(Argument("acodec", type))
        fun forceFormat(format: String) = insert(Argument("f", format))
    }
    private fun buildCommand(): String {
        var result = ffmpegBin
        Builder().apply(argsBuilder).arguments.forEach { result += " $it" }
        return result
    }
    private fun getOutputFile(): File = newTempFile(suffix=".${outputFormat.ext}")
    private fun getResultBlocking(): File? {
        checkFFMpeg()
        val result = getOutputFile()
        var command = buildCommand()
        command += " ${result.absolutePath}"
        println(command)
        return try {
            runBlocking {
                ProgramExecutor(command) {
                    environment {
                        append(ffmpegPath)
                    }
                }.start()
            }
            result
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getResult(): File? = withContext(Dispatchers.IO) {
        getResultBlocking()
    }
    companion object {
        var ffmpegBin: String = "ffmpeg"
        var ffmpegPath = ""
        fun checkFFMpeg() {
            if (!File(ffmpegBin).exists())
                println("Warn: FFMpeg does not exist")
        }
    }
}