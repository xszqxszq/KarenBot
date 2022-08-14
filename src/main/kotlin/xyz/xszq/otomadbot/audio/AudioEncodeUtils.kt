package xyz.xszq.otomadbot.audio

import com.soywiz.korau.format.readSoundInfo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import io.github.kasukusakura.silkcodec.SilkCoder
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.message.data.Audio
import net.mamoe.mirai.message.data.OnlineAudio
import xyz.xszq.otomadbot.FFMpegFileType
import xyz.xszq.otomadbot.FFMpegTask
import xyz.xszq.otomadbot.NetworkUtils
import xyz.xszq.otomadbot.kotlin.newTempFile
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
    private suspend fun anyToMp3BeforeSilk(file: File): File? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
        if (file.getAudioDuration() < 1.0)
            audioFilter("apad=pad_dur=3")
        else if (file.getAudioDuration() < 2.0)
            audioFilter("apad=pad_dur=2")
        audioRate("24k")
        audioChannels(1)
    }.getResult()
    suspend fun anyToMp3(file: File): File? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
    }.getResult()
    private suspend fun anyToWavBeforePy(file: File): File? = FFMpegTask(FFMpegFileType.WAV) {
        input(file)
        acodec("pcm_s16le")
        audioRate("44100")
        yes()
    }.getResult()
    suspend fun anyToWav(file: File) = anyToWavBeforePy(file)
    suspend fun cropPeriod(file: File, startPoint: Double,
                           duration: Double, forSilk: Boolean = true): File? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
        startAt(startPoint)
        duration(duration)
        if (forSilk) {
            audioRate("24k")
            audioChannels(1)
        }
    }.getResult()
    suspend fun silkToWav(silk: ByteArray): File? {
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
    suspend fun prepareAudio(file: File): File? = FFMpegTask(FFMpegFileType.WAV) {
        input(file)
        acodec("pcm_s16le")
        audioRate("24k")
        audioChannels(1)
        yes()
    }.getResult()
}

suspend fun Audio.toWav() =
    AudioEncodeUtils.silkToWav(NetworkUtils.downloadAsByteArray((this as OnlineAudio).urlForDownload))