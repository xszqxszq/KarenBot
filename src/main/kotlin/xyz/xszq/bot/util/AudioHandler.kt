package xyz.xszq.bot.util

import io.github.kasukusakura.silkcodec.SilkCoder
import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.async.use
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.std.toVfs
import korlibs.io.stream.*
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.ffmpeg.FFProbe
import java.io.File

/**
 * 处理 WAV 音频
 */
object AudioHandler {
    private val logger = KotlinLogging.logger {}
    /**
     * 从 WAV 文件读取 PCM 数据
     * @param file WAV 文件
     */
    suspend fun readWaveFile(file: VfsFile): ByteArray {
        return file.openInputStream().use { stream ->
            // RIFF 头
            require(stream.readString(4) == "RIFF") { "Invalid RIFF file" }
            stream.skip(4)
            require(stream.readString(4) == "WAVE") { "Invalid WAVE file" }

            // 跳到实际数据部分
            while (stream.readString(4) != "data") {
                stream.skip(stream.readS32LE())
            }
            val dataSize = stream.readS32LE()
            stream.readBytesUpTo(dataSize)
        }
    }
    /**
     * 合并多个 WAV 文件
     * @param inputFiles 输入文件列表
     * @param outputFile 输出文件
     * @param pcm 是否输出 PCM 文件
     * @param sampleRate 目标采样率
     * @param numChannels 目标声道数
     * @param bit 目标比特率
     */
    suspend fun mergeWaveFiles(
        inputFiles: List<VfsFile>,
        outputFile: VfsFile,
        pcm: Boolean = false,
        sampleRate: Int = 24000,
        numChannels: Int = 1,
        bit: Int = 16
    ) {
        val data = inputFiles.map { file -> readWaveFile(file) }
        val totalSize = data.sumOf { it.size }

        outputFile.open(VfsOpenMode.WRITE).use { stream ->
            if (!pcm) {
                // RIFF 头
                stream.writeString("RIFF")
                stream.write32LE(36 + totalSize)
                stream.writeString("WAVE")

                // 数据格式区
                stream.writeString("fmt ")
                // 块大小
                stream.write32LE(16)
                // 音频格式类型：PCM(1)
                stream.write16LE(1)
                // 声道数
                stream.write16LE(numChannels)
                // 采样率
                stream.write32LE(sampleRate)
                // 每秒字节数
                stream.write32LE((sampleRate * numChannels * bit) / 8)
                // 每块字节数
                stream.write16LE(numChannels * bit / 8)
                // 每采样位数
                stream.write16LE(bit)

                // 数据区
                stream.writeString("data")
                stream.write32LE(totalSize)
            }
            data.forEach { stream.write(it) }
        }
    }

    /**
     * 将 PCM 音频转为 Silk
     * @param pcmFile 输入文件
     * @param bitRate 比特率
     * @param sampleRate 采样率
     */
    fun pcmToSilk(pcmFile: VfsFile, bitRate: Int = 24000, sampleRate: Int = 24000): VfsFile {
        val silkFile = File(newTempFile(suffix=".silk").absolutePath)
        File(pcmFile.absolutePath).inputStream().use { pcm ->
            silkFile.outputStream().use { silk ->
                SilkCoder.encode(pcm, silk, sampleRate, bitRate)
            }
        }
        return silkFile.toVfs()
    }
    suspend inline fun VfsFile.crop(
        start: Double,
        duration: Double,
        block: suspend (VfsFile) -> Unit
    ) {
        val cropped = FFMpegTask(FFMpegFileType.PCM) {
            input(absolutePath)
            yes()
            forceFormat("s16le")
            audioCodec("pcm_s16le")
            logLevel("warning")
            startAt(start)
            duration(duration)
            audioRate("24k")
            audioChannels(1)
        }.result()
        try {
            block(cropped)
        } finally {
            cropped.delete()
        }
    }
    suspend fun VfsFile.duration() = runCatching {
        FFProbe(File(this.absolutePath)).getResult().format?.duration?.toDouble()
    }.onFailure {
        logger.warn { "ffprobe 获取音频时长失败: ${it.message}" }
    }.getOrNull()
}