package xyz.xszq.bot.ffmpeg

import korlibs.io.file.VfsFile
import kotlinx.serialization.json.Json
import xyz.xszq.nereides.newTempFile

class FFProbe(
    private val target: VfsFile,
    private val showStreams: Boolean = false,
    private val showFormat: Boolean = true
) {
    suspend fun getResult(): FFProbeResult {
        val result = newTempFile(suffix=".json")
        ProgramExecutor(buildString {
            append(ffprobeBin)
            append(" \"${target.absolutePath}\"")
            append(" -print_format json")
            if (showStreams)
                append(" -show_streams")
            if (showFormat)
                append(" -show_format")
            append(" > \"${result.absolutePath}\"")
        }) {
            environment {
                append(ffprobePath)
            }
        }.start()
        return json.decodeFromString(result.readText())
    }
    companion object {
        var ffprobeBin: String = "ffprobe"
        var ffprobePath = ""
        val json = Json {
            ignoreUnknownKeys = true
        }
    }
}