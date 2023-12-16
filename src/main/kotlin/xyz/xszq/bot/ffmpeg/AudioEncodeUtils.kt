package xyz.xszq.bot.ffmpeg

import io.github.kasukusakura.silkcodec.SilkCoder
import korlibs.audio.format.readSoundInfo
import korlibs.io.file.VfsFile
import korlibs.io.file.std.toVfs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.xszq.nereides.newTempFile
import java.io.File


suspend fun VfsFile.getAudioDuration(): Double {
    return readSoundInfo()?.duration?.seconds ?: FFProbe(
        this, showStreams = false, showFormat = true
    ).getResult().format!!.duration.toDouble()
}
fun File.getAudioDuration(): Double = runBlocking {
    withContext(Dispatchers.IO) {
        toVfs().getAudioDuration()
    }
}

suspend fun File.toMp3BeforeSilk(): File? =
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
suspend fun File.toMp3(): File? =
    FFMpegTask(FFMpegFileType.MP3) {
        input(this@toMp3)
        yes()
    }.getResult()
suspend fun File.toPCM(): File? =
    FFMpegTask(FFMpegFileType.PCM) {
        input(this@toPCM)
        forceFormat("s16le")
        acodec("pcm_s16le")
        audioRate("24k")
        audioChannels(1)
        yes()
    }.getResult()
suspend fun File.cropPeriod(
    startPoint: Double,
    duration: Double,
    forSilk: Boolean = true
): File? =
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
suspend fun VfsFile.cropPeriod(
    startPoint: Double,
    duration: Double,
    forSilk: Boolean = true
) = File(absolutePath).cropPeriod(startPoint, duration, forSilk)?.toVfs()
suspend fun File.toSilk(): File {
    val silk = newTempFile(suffix = ".silk")
    silk.outputStream().use { os ->
        toMp3BeforeSilk() ?.toPCM() ?.apply {
            SilkCoder.encode(
                inputStream(),
                os,
                24000
            )
        }
    }
    silk.deleteOnExit()
    return silk
}
suspend fun VfsFile.toSilk() = File(absolutePath).toSilk()