package xyz.xszq.bot.ffmpeg

import korlibs.io.file.VfsFile
import korlibs.io.file.std.toVfs
import java.io.File
import kotlin.io.path.createTempFile

@Suppress("unused")
class FFMpegTask(
    private val outputFormat: FFMpegFileType,
    private val argsBuilder: Builder.() -> Unit
) {
    @DslMarker annotation class FFMpegBuilder
    @FFMpegBuilder
    class Builder {
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
        fun videoFilter(filter: String) = insert(Argument("vf", filter))
        fun audioCodec(type: String) = insert(Argument("c:a", type))
        fun videoCodec(type: String) = insert(Argument("c:v", type))
        fun forceFormat(format: String) = insert(Argument("f", format))
        fun filterComplex(filter: String) = insert(Argument("filter_complex", filter))
        fun map(map: String) = insert(Argument("map", map))
        fun frameRate(rate: Double) = insert(Argument("framerate", rate.toString()))
        fun vFrames(frames: Int) = insert(Argument("vframes", frames.toString()))
        fun preset(preset: String) = insert(Argument("preset", preset))
        fun hlsTime(time: Double) = insert(Argument("hls_time", time.toString()))
        fun hlsSegmentFilename(filename: String) = insert(Argument("hls_segment_filename", filename))
        fun hlsListSize(size: Int) = insert(Argument("hls_list_size", size.toString()))
        fun logLevel(level: String) = insert(Argument("loglevel", level))
    }
    private fun buildCommand(result: String): List<String> {
        return buildList {
            add(ffmpegBin)
            Builder().apply { argsBuilder(this@apply) }.arguments.forEach {
                addAll(it.toList())
            }
            add(result)
        }
    }
    fun getResult(output: File = createTempFile(suffix = ".${outputFormat.ext}").toFile()): File {
        val command = buildCommand(output.absolutePath)
        ProgramExecutor(command, false) {
            environment {
                append(ffmpegPath)
            }
        }.start()
        return output
    }
    fun result(output: VfsFile = createTempFile(suffix = ".${outputFormat.ext}").toFile().toVfs()): VfsFile {
        val command = buildCommand(output.absolutePath)
        ProgramExecutor(command, false) {
            environment {
                append(ffmpegPath)
            }
        }.start()
        return output
    }
    companion object {
        var ffmpegBin: String = "/root/bin/ffmpeg"
        var ffmpegPath = ""
    }
}