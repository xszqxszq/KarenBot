@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package tk.xszq.otomadbot.audio

import io.github.mzdluo123.silk4j.AudioUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.xszq.otomadbot.getAudioDuration
import tk.xszq.otomadbot.lowercase
import java.io.File

object AudioEncodeUtils {
    private suspend fun anyToMp3BeforeSilk(file: File): File? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
        if (file.getAudioDuration() < 1.0)
            audioFilter("apad=pad_dur=3")
        else if (file.getAudioDuration() < 2.0)
            audioFilter("apad=pad_dur=2")
        audioRate(24000)
        audioChannels(1)
    }.getResult()
    suspend fun anyToMp3(file: File): File? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
    }.getResult()
    private suspend fun anyToWavBeforePy(file: File): File? = FFMpegTask(FFMpegFileType.WAV) {
        input(file)
        yes()
    }.getResult()
    suspend fun anyToWav(file: File) = if (file.extension.lowercase() == "wav") file else anyToWavBeforePy(file)
    suspend fun cropPeriod(file: File, startPoint: Double,
                           duration: Double, forSilk: Boolean = true): File? = FFMpegTask(FFMpegFileType.MP3) {
        input(file)
        yes()
        startAt(startPoint)
        duration(duration)
        if (forSilk) {
            audioRate(24000)
            audioChannels(1)
        }
    }.getResult()
    fun mp3ToSilkBlocking(file: File): File = AudioUtils.mp3ToSilk(file)
    suspend fun mp3ToSilk(file: File): File = withContext(Dispatchers.IO) {
        mp3ToSilkBlocking(file)
    }
    suspend fun convertAnyToSilk(file: File): File? {
        if (file.extension == "silk") return file
        val mp3 = anyToMp3BeforeSilk(file)
        val result = mp3?.let { mp3ToSilk(it) }
        mp3?.delete()
        return result
    }
}