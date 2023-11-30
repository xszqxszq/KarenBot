package xyz.xszq.bot.ffmpeg

import com.soywiz.korau.format.readSoundInfo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import io.github.kasukusakura.silkcodec.SilkCoder
import kotlinx.coroutines.runBlocking
import xyz.xszq.nereides.newTempFile
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