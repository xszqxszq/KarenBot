package xyz.xszq.otomadbot.audio

import com.soywiz.korau.format.readSoundInfo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import io.github.kasukusakura.silkcodec.SilkCoder
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.Audio
import net.mamoe.mirai.message.data.OnlineAudio
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.ffmpeg.FFMpegFileType
import xyz.xszq.otomadbot.ffmpeg.FFMpegTask
import xyz.xszq.otomadbot.kotlin.getTempFile
import xyz.xszq.otomadbot.kotlin.newTempFile
import xyz.xszq.otomadbot.kotlin.useTempFile
import java.io.File

suspend fun getAudioDuration(file: File): Double {
    return file.toVfs().readSoundInfo()?.duration?.seconds ?: 0.0
}
suspend fun VfsFile.getAudioDuration(): Double {
    return readSoundInfo()?.duration?.seconds ?: 0.0
}
fun File.getAudioDuration(): Double = runBlocking {
    toVfs().readSoundInfo()?.duration?.seconds ?: 0.0
}

object AudioEncodeUtils {
    private suspend fun anyToWavBeforePy(file: VfsFile): VfsFile = FFMpegTask(FFMpegFileType.WAV) {
        input(file)
        acodec("pcm_s16le")
        audioRate("44100")
        yes()
    }.getResult()
    suspend fun anyToWav(file: VfsFile) = anyToWavBeforePy(file)
    suspend fun cropPeriod(file: VfsFile, startPoint: Double,
                           duration: Double, forSilk: Boolean = true): VfsFile? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
        startAt(startPoint)
        duration(duration)
        if (forSilk) {
            audioRate("24k")
            audioChannels(1)
        }
    }.getResult()
    suspend fun silkToWav(silk: ByteArray): VfsFile {
        val pcm = newTempFile(suffix = ".pcm")
        pcm.outputStream().use {
            SilkCoder.decode(silk.inputStream(), it)
        }
        pcm.deleteOnExit()
        return FFMpegTask(FFMpegFileType.WAV) {
            forceFormat("s16le")
            audioRate("24k")
            audioChannels(1)
            input(pcm.absolutePath)
            yes()
        }.getResult()
    }
}

suspend fun Audio.toWav() =
    AudioEncodeUtils.silkToWav(NetworkUtils.downloadAsByteArray((this as OnlineAudio).urlForDownload))

suspend fun VfsFile.toMp3BeforeSilk() =
    FFMpegTask(FFMpegFileType.MP3) {
        input(this@toMp3BeforeSilk)
        yes()
        if (getAudioDuration() < 1.0)
            audioFilter("apad=pad_dur=3")
        else if (getAudioDuration() < 2.0)
            audioFilter("apad=pad_dur=2")
        audioRate("24k")
        audioChannels(1)
    }.getResult()
suspend fun VfsFile.toPCM() =
    FFMpegTask(FFMpegFileType.PCM) {
        input(this@toPCM)
        forceFormat("s16le")
        acodec("pcm_s16le")
        audioRate("24k")
        audioChannels(1)
        yes()
    }.getResult()
suspend fun VfsFile.toSilk(): VfsFile {
    val silk = getTempFile(suffix = ".silk")
    File(silk.absolutePath).outputStream().use { outputStream ->
        toMp3BeforeSilk().useTempFile { mp3 ->
            mp3.toPCM().useTempFile { pcm ->
                File(pcm.absolutePath).inputStream().use { inputStream ->
                    SilkCoder.encode(inputStream, outputStream, 24000)
                }
            }
        }
    }
    return silk
}