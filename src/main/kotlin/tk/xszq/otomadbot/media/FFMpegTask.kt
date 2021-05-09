@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package tk.xszq.otomadbot.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import tk.xszq.otomadbot.bot
import tk.xszq.otomadbot.configMain
import tk.xszq.otomadbot.newTempFileBlocking
import java.io.File

data class Argument(val key: String, val value: String = "") {
    override fun toString(): String {
        var result = "-$key"
        if (value.isNotBlank())
            result += " $value"
        return result
    }
    fun toTypedArray(): Array<String> {
        val result = mutableListOf("-$key")
        if (value.isNotBlank())
            result.add(value)
        return result.toTypedArray()
    }
}
data class FFMpegFileType(val ext: String, val requiredArgs: List<Argument> = emptyList()) {
    companion object {
        val MP3 = FFMpegFileType("mp3")
        val WAV = FFMpegFileType("wav")
    }
}
class FFMpegTask(val outputFormat: FFMpegFileType,
                 val argsBuilder: Builder.() -> Unit) {
    @DslMarker annotation class FFMpegBuilder
    @FFMpegBuilder
    inner class Builder {
        val arguments = mutableListOf<Argument>()
        private fun insert(arg: Argument) = arguments.add(arg)
        fun input(path: String) = insert(Argument("i", path))
        fun input(path: File) = insert(Argument("i", path.absolutePath))
        fun startAt(timeInSecond: Double) = insert(Argument("ss", timeInSecond.toString()))
        fun duration(timeInSecond: Double) = insert(Argument("t", timeInSecond.toString()))
        fun audioRate(rate: Int) = insert(Argument("ar", rate.toString()))
        fun audioChannels(channels: Int) = insert(Argument("ac", channels.toString()))
        fun yes() = insert(Argument("y"))
        fun audioFilter(filter: String) = insert(Argument("af", filter))
    }
    var ffmpegPath: String = configMain.bin["ffmpeg"] ?: "ffmpeg"
    var ffmpegEnv = arrayOf(configMain.bin["ffmpegpath"])
    fun buildCommand(): String {
        var result = ffmpegPath
        Builder().apply(argsBuilder).arguments.forEach { result += " $it" }
        return result
    }
    private fun getOutputFile(): File = newTempFileBlocking(suffix=".${outputFormat.ext}")
    private fun getResultBlocking(): File? {
        checkFFMpeg(ffmpegPath)
        val result = getOutputFile()
        var command = buildCommand()
        command += " ${result.absolutePath}"
        bot!!.logger.debug(command)
        return try {
            runBlocking {
                ProgramExecutor(command) {
                    environment {
                        append(configMain.bin["ffmpegpath"])
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
        fun checkFFMpeg(path: String = configMain.bin["ffmpeg"] ?: "ffmpeg") {
            if (!File(path).exists())
                println("Warn: FFMpeg does not exist")
        }
    }
}