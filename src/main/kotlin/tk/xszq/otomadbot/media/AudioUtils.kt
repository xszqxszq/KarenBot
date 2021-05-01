@file:Suppress("unused")
package tk.xszq.otomadbot.media

import com.soywiz.korau.format.readSoundInfo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.toVfs
import com.soywiz.korio.net.QueryString
import com.soywiz.korio.util.UUID
import io.github.mzdluo123.silk4j.AudioUtils
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.whileSelectMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsVoice
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.RemoteFile.Companion.sendFile
import org.jsoup.Jsoup
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.PyApi
import tk.xszq.otomadbot.database.RandomMusic
import tk.xszq.otomadbot.database.getCooldown
import java.io.*
import java.util.*


suspend fun getAudioDuration(file: File): Double {
    return file.toVfs().readSoundInfo()?.duration?.seconds ?: 0.0
}
suspend fun VfsFile.getAudioDuration(): Double {
    return readSoundInfo()?.duration?.seconds ?: 0.0
}
fun File.getAudioDuration(): Double = runBlocking {
    toVfs().readSoundInfo()?.duration?.seconds ?: 0.0
}

@Suppress("UNUSED_PARAMETER")
object AudioUtils: CommandUtils("audio") {
    @CommandSingleArg("帮我修音", "shift", true)
    suspend fun doHandleShift(path: String, event: GroupMessageEvent) = event.run {
        if (path.isBlank()) {
            quoteReply("请指定欲修音的文件名（仅支持WAV格式）！")
        } else {
            val file = group.filesRoot.resolve(path)
            if (!file.exists()) {
                quoteReply("文件不存在，请检查是否有拼写错误")
            } else if (file.length() > 10485760L) {
                quoteReply("文件不得超过10M")
            } else {
                quoteReply("正在处理中，请稍等片刻……")
                val url = file.getDownloadInfo()!!.url
                val id = UUID.randomUUID().toString()
                val raw = downloadFile(url, "$id.${File(file.name).extension}")
                if (raw.getAudioDuration() > 10.0) {
                    quoteReply("文件不得超过10s")
                } else {
                    val before = AudioEncodeUtils.anyToWav(raw)!!
                    val command = "${configMain.bin["python"]} ${configMain.bin["pitchshift"]} " +
                            before.absolutePath
                    bot.logger.debug(command)
                    ProgramExecutor(command).start()
                    val result = File(before.absolutePath + ".result.wav")
                    result.toExternalResource().use {
                        try {
                            group.sendFile("$id.wav", it)
                            quoteReply("修音成功，该文件将在10min内被撤回。")
                            delay(600000)
                            group.filesRoot.resolve("$id.wav").delete()
                        } catch (e: Exception) {
                            quoteReply("文件上传失败")
                        }
                    }
                    raw.delete()
                    result.delete()
                }
            }
        }
    }
    @CommandSingleArg("朗读", "tts", true)
    suspend fun doHandleTTS(text: String, event: GroupMessageEvent) = event.run {
        if (text.isNotBlank()) {
            val lang = PyApi().getLanguage(text)
            val filename = UUID.randomUUID().toString()
            val original = lang?.let {
                if (lang == "ja") TTSDownloader.yukkuri(text, filename)
                else TTSDownloader.googleTranslate(text, filename, it)
            } ?: run { TTSDownloader.googleTranslate(text, filename) }
            val encoded = AudioEncodeUtils.convertAnyToSilk(original)
            encoded ?.let { file ->
                file.toExternalResource().use {
                    group.sendMessage(group.uploadVoice(it))
                }
            }
            original.delete()
            encoded?.delete()
        }
    }
    @CommandMatching("randomtouhou", "touhou.random", true)
    suspend fun touhouRandomMusic(matched: MatchResult, event: GroupMessageEvent) = event.run {
        RandomMusic("touhou").init().getRandomPeriod()?.let { song ->
            group.sendMessage(AudioEncodeUtils.mp3ToSilk(song).toExternalResource().uploadAsVoice(group))
            song.delete()
        }
    }
    @CommandFinding("midi", "midishow", false)
    @MiraiExperimentalApi
    suspend fun doHandleMIDI(matched: MatchResult, event: MessageEvent) = event.run {
        matched.groupValues.lastOrNull{ i->i.isNotBlank() }?.let { keyword ->
            val client = HttpClient(CIO)
            val response = client.get<HttpResponse>("https://www.midishow.com/search/result?q=$keyword") {
                headers {
                    append("Referer", "https://www.midishow.com/")
                    append("User-Agent", availableUA)
                }
            }
            if (response.isSuccessful()) {
                val results = Jsoup.parse(response.readText()).select("#search-result>div")
                if (results.size == 0 || results[0].attr("data-key") == "") {
                    quoteReply("没有找到相关MIDI……")
                } else {
                    val title = results[0].select(".text-hover-primary").text()
                    val summary = """上传用户：${results[0].select(".avatar-img").attr("alt")}
                        乐曲时长：${results[0].select("[title=\"乐曲时长\"]").text()}
                        音轨数量：${results[0].select("[title=\"音轨数量\"]").text()}""".trimLiteralTrident()
                    subject.sendMessage(SimpleServiceMessage(60, QQXMLMessage {
                        brief("[MIDI]$title")
                        url("https://www.midishow.com/midi/${results[0].attr("data-key")}.html")
                        title("$title :: MidiShow")
                        summary(summary)
                        cover("https://www.midishow.com/favicon.ico")
                    }.build()))
                }
            } else {
                quoteReply("网络错误，MIDI搜索失败")
            }
        }
    }
    @Suppress("CAST_NEVER_SUCCEEDS")
    @Command("猜东方原曲", "touhou.guess", true)
    suspend fun touhouGuessMusic(args: Args, event: GroupMessageEvent) = event.run {
        val currentStatus = event.getCooldown("touhou")
        currentStatus.onReady {
            currentStatus.update()
            val song = RandomMusic("touhou").init()
            song.getRandomPeriod(5.0)?.let { raw ->
                val file = AudioEncodeUtils.mp3ToSilk(raw)
                file.toExternalResource().use {
                    group.sendMessage(it.uploadAsVoice(group))
                }
                raw.delete()
                file.delete()
            }
            val possibleAnswers = song.parsePossibleAnswers()
            group.sendMessage("猜一猜这首东方原曲叫什么~\n一分钟后没人猜出将揭晓答案哦~")
            whileSelectMessages(filterContext = false) {
                default {
                    val answer = this as GroupMessageEvent
                    if (answer.group.id == group.id) {
                        var correct = false
                        listOf(answer.message.content, PyApi().getPinyin(answer.message.content.toLowerCase())!!,
                            answer.message.content.toLowerCase()
                        ).forEach loop@{ name ->
                            possibleAnswers.forEach {
                                if (name in it && name.length > 2) {
                                    correct = true
                                    return@loop
                                }
                            }
                        }
                        if (correct)
                            answer.quoteReply("恭喜答对了~\n这首曲子是${song.music.name}")
                        !correct
                    } else {
                        true
                    }
                }
                timeout(60000) {
                    group.sendMessage("很遗憾，没有人答对~\n这首曲子是${song.music.name}")
                    false
                }
            }
            currentStatus.set(0L)
        } ?: run {
            event.quoteReply("本局猜东方原曲还没有结束哦~")
        }
    }
    @CommandSingleArg("测bpm", "bpm", true)
    suspend fun analyseBPM(file: String, event: GroupMessageEvent) = event.run {
        if (file.isBlank()) {
            quoteReply("使用方法：测bpm 群文件名")
            return@run
        }
        val targetFile = group.filesRoot.resolve(file)
        if (getDownloadFileSize(targetFile.getDownloadInfo()!!.url) >= 20971520L) {
            quoteReply("文件大小请勿超过20M :(")
            return@run
        }
        val target = downloadFile(targetFile.getDownloadInfo()!!.url, UUID.randomUUID().toString(),
            targetFile.name.split(".").last())
        val before = AudioEncodeUtils.anyToWav(target)!!
        quoteReply(PyApi().getBPM(before.absolutePath)!!.toString())
        before.delete()
        target.delete()
    }
}

object TTSDownloader {
    /**
     * Download TTS result from yukkuritalk.com.
     * @param text Text to speak
     * @param filename Filename for the voice
     * @param mode Yukkuri speak mode
     */
    suspend fun yukkuri(text: String, filename: String, mode: String = "ゆっくり言っていてね"): File? {
        val url = "http://yukkuritalk.com/?" + QueryString.encode(Pair("txt", text), Pair("submit", mode))
        val client = HttpClient(CIO)
        val response = client.get<HttpResponse>(url)
        return if (response.isSuccessful()) {
            val doc = Jsoup.parse(response.readText())
            val audio = doc.select(".translate_yukkuri>audio")
            if (audio.isNotEmpty())
                downloadFile(
                    "http://yukkuritalk.com/" + audio.first().attr("src"), filename, ".wav",
                    requiredHeaders = listOf(
                        Pair("Referer", "http://yukkuritalk.com/"),
                        Pair("User-Agent", availableUA)
                    ))
            else null
        } else null
    }
    /**
     * Download TTS result from Google.
     * @param text Text to speak
     * @param filename Filename for the voice
     * @param lang Language of the text
     */
    suspend fun googleTranslate(text: String, filename: String, lang: String = "zh-CN") = downloadFile(
        "http://translate.google.cn/translate_tts?" + QueryString.encode(
            Pair("ie", "UTF-8"), Pair("total", "1"), Pair("idx", "0"), Pair("textlen",
                if (text.length < 10) "16" else text.length.toString()),
            Pair("client", "tw-ob"), Pair("q", text), Pair("tl", lang)
        ), filename, ".mp3", requiredHeaders = listOf(
            Pair("Referer", "http://translate.google.cn/"),
            Pair("User-Agent", "stagefright/1.2 (Linux;Android 5.0)")
        )
    )
}

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
    suspend fun anyToWav(file: File) = if (file.extension.toLowerCase() == "wav") file else anyToWavBeforePy(file)
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
    private fun mp3ToSilkBlocking(file: File): File = AudioUtils.mp3ToSilk(file)
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