package xyz.xszq.bot.ffmpeg

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempFile

/**
 * FFProbe 任务
 *
 * 用于读取媒体文件的元数据信息
 *
 * @param target 目标媒体文件
 * @param showStreams 是否读取媒体流信息
 * @param showFormat 是否读取容器格式信息
 */
class FFProbe(
    private val target: File,
    private val showStreams: Boolean = false,
    private val showFormat: Boolean = true
) {
    /**
     * 读取信息并得到结果
     *
     * @return 媒体信息
     */
    suspend fun getResult(): FFProbeResult {
        return createTempFile(suffix = ".json").toFile().let { file ->
            val command = buildList {
                add(ffprobeBin)
                add(target.absolutePath)
                add("-print_format")
                add("json")
                if (showStreams)
                    add("-show_streams")
                if (showFormat)
                    add("-show_format")
            }
            ProgramExecutor(command, false) {
                outputFile = file
                environment {
                    append(ffprobePath)
                }
            }.start()
            json.decodeFromString(file.readText().also {
                file.delete()
            })
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