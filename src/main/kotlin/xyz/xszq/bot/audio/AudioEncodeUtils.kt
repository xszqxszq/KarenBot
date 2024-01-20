package xyz.xszq.bot.audio

import io.github.kasukusakura.silkcodec.SilkCoder
import korlibs.audio.format.readSoundInfo
import korlibs.io.file.VfsFile
import kotlinx.coroutines.sync.Semaphore
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.bot.ffmpeg.FFProbe
import xyz.xszq.nereides.getTempFile
import xyz.xszq.nereides.useTempFile
import java.io.File

val audioSemaphore = Semaphore(64)

suspend fun VfsFile.getAudioDuration(): Double {
    return readSoundInfo()?.duration?.seconds ?: FFProbe(
        this, showStreams = false, showFormat = true
    ).getResult().format!!.duration.toDouble()
}

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
suspend fun VfsFile.toMp3() =
    FFMpegTask(FFMpegFileType.MP3) {
        input(this@toMp3)
        yes()
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
suspend fun VfsFile.cropPeriod(
    startPoint: Double,
    duration: Double,
    forSilk: Boolean = true
) =
    FFMpegTask(FFMpegFileType.MP3) {
        input(this@cropPeriod)
        yes()
        startAt(startPoint)
        duration(duration)
        if (forSilk) {
            audioRate("24k")
            audioChannels(1)
        }
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