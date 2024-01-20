package xyz.xszq.bot.ffmpeg

import korlibs.io.file.VfsFile
import kotlinx.serialization.json.Json
import xyz.xszq.nereides.useTempFile

class FFProbe(
    private val target: VfsFile,
    private val showStreams: Boolean = false,
    private val showFormat: Boolean = true
) {
    suspend fun getResult(): FFProbeResult {
        return useTempFile(suffix=".json") {
            ProgramExecutor(buildList {
                add(ffprobeBin)
                add("\"${target.absolutePath}\"")
                add("-print_format json")
                if (showStreams)
                    add("-show_streams")
                if (showFormat)
                    add("-show_format")
                add("> \"${it.absolutePath}\"")
            }) {
                environment {
                    append(ffprobePath)
                }
            }.start()
            json.decodeFromString(it.readString())
        }
    }
    companion object {
        var ffprobeBin: String = "ffprobe"
        var ffprobePath = ""
        val json = Json {
            ignoreUnknownKeys = true
        }
    }
}