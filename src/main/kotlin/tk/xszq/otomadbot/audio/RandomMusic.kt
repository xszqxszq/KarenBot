@file:Suppress("MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot.audio

import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import tk.xszq.otomadbot.EventHandler
import tk.xszq.otomadbot.core.OtomadBotCore
import tk.xszq.otomadbot.equalsTo
import tk.xszq.otomadbot.getAudioDuration
import tk.xszq.otomadbot.startsWithSimple
import java.io.File
import kotlin.random.Random

object RandomMusic: EventHandler("随机音乐", "audio.random") {
    private val audioExts = listOf("mp3", "wav", "ogg", "m4a")
    override fun register() {
        GlobalEventChannel.subscribeGroupMessages {
            equalsTo("随机东方原曲") {
                subject.sendMessage(fetchVoice("touhou", this))
            }
            startsWithSimple("随机maimai") { _, _ ->
                subject.sendMessage(fetchVoice("finale", this))
            }
            startsWithSimple("随机dx") { _, _ ->
                subject.sendMessage(fetchVoice("dx", this))
            }
        }
        super.register()
    }
    fun fetchRandom(type: String): File = OtomadBotCore.configFolder.resolve("music/$type").listFiles()!!
        .filter { it.extension in audioExts }.random()
    suspend fun fetchVoice(type: String, event: MessageEvent) = event.run {
        val raw = fetchRandom(type)
        println(raw.absolutePath)
        println(raw.exists())
        val before = getRandomPeriod(raw)!!
        val silk = AudioEncodeUtils.convertAnyToSilk(before)!!
        val result = silk.toExternalResource().use {
            (subject as AudioSupported).uploadAudio(it)
        }
        before.delete()
        silk.delete()
        return@run result
    }
    const val defaultMinDuration = 15.0
    suspend fun getRandomPeriod(file: File, duration: Double = defaultMinDuration): File? = AudioEncodeUtils.cropPeriod(
        file, Random.nextDouble(0.0, file.getAudioDuration() - duration), duration)
}