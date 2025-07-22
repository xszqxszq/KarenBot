package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempFile

class FFProbe(
    private val target: File,
    private val showStreams: Boolean = false,
    private val showFormat: Boolean = true
) {
    fun getResult(): FFProbeResult {
        return createTempFile(suffix = ".json").toFile().let {
            ProgramExecutor(buildList {
                add(ffprobeBin)
                add("\"${target.absolutePath}\"")
                add("-print_format json")
                if (showStreams)
                    add("-show_streams")
                if (showFormat)
                    add("-show_format")
                add("> \"${it.absolutePath}\"")
            }, showOutput = false) {
                environment {
                    append(ffprobePath)
                }
            }.start()
            json.decodeFromString(it.readText())
        }
    }
    companion object {
        var ffprobeBin: String = "/root/bin/ffprobe"
        var ffprobePath = ""
        val json = Json {
            ignoreUnknownKeys = true
        }
    }
}