package xyz.xszq.bot

import io.github.kasukusakura.silkcodec.SilkCoder
import korlibs.io.async.use
import korlibs.io.file.VfsFile
import korlibs.io.file.VfsOpenMode
import korlibs.io.file.std.toVfs
import korlibs.io.stream.readBytesUpTo
import korlibs.io.stream.readS32LE
import korlibs.io.stream.readString
import korlibs.io.stream.skip
import korlibs.io.stream.write16LE
import korlibs.io.stream.write32LE
import korlibs.io.stream.writeString
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.ffmpeg.FFProbe
import java.io.File

/**
 * Process wave files.
 */
object AudioHandler {
    /**
     * Read PCM part from wave file.
     * @param file Wave file.
     */
    suspend fun readWaveFile(file: VfsFile): ByteArray {
        return file.openInputStream().use { stream ->
            // RIFF Header part
            require(stream.readString(4) == "RIFF") { "Invalid RIFF file" }
            stream.skip(4)
            require(stream.readString(4) == "WAVE") { "Invalid WAVE file" }

            // Skip to data part
            while (stream.readString(4) != "data") {
                stream.skip(stream.readS32LE())
            }
            val dataSize = stream.readS32LE()
            stream.readBytesUpTo(dataSize)
        }
    }
    /**
     * Merge wave files to a new file.
     * @param inputFiles Input Wave files.
     * @param outputFile Output Wave file.
     * @param pcm Whether output a PCM file or a WAV file.
     * @param sampleRate Target sampling rate.
     * @param numChannels Target channels.
     * @param bit Target bits per second.
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
                // RIFF Header part
                stream.writeString("RIFF")
                stream.write32LE(36 + totalSize)
                stream.writeString("WAVE")

                // Data format part
                stream.writeString("fmt ")
                // Block size
                stream.write32LE(16)
                // Audio format type: PCM(1)
                stream.write16LE(1)
                // Channels
                stream.write16LE(numChannels)
                // Sampling rate
                stream.write32LE(sampleRate)
                // Bytes per second
                stream.write32LE((sampleRate * numChannels * bit) / 8)
                // Bytes per block
                stream.write16LE(numChannels * bit / 8)
                // Bits per sample
                stream.write16LE(bit)

                // Data part
                stream.writeString("data")
                stream.write32LE(totalSize)
            }
            data.forEach { stream.write(it) }
        }
    }

    /**
     * Convert PCM to Silk.
     * @param pcmFile The PCM file to convert.
     * @param bitRate Bit rate.
     * @param sampleRate Sample Rate.
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
    fun VfsFile.crop(
        start: Double,
        duration: Double
    ) = FFMpegTask(FFMpegFileType.PCM) {
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
    fun VfsFile.duration() = FFProbe(File(this.absolutePath)).getResult().format?.duration?.toDouble()
}