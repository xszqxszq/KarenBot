package xyz.xszq.otomadbot.audio

import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseNameWithoutCompoundExtension
import com.soywiz.korio.file.std.toVfs
import kotlinx.coroutines.flow.firstOrNull
import net.mamoe.mirai.contact.AudioSupported
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import xyz.xszq.OtomadBotCore
import xyz.xszq.events
import xyz.xszq.otomadbot.CommandModule
import xyz.xszq.otomadbot.CommonCommandWithArgs
import xyz.xszq.otomadbot.Cooldown
import xyz.xszq.otomadbot.ffmpeg.FFMpegFileType
import xyz.xszq.otomadbot.ffmpeg.FFMpegTask
import xyz.xszq.otomadbot.ifReady
import xyz.xszq.otomadbot.kotlin.toFile
import xyz.xszq.otomadbot.kotlin.toPinyinList
import xyz.xszq.otomadbot.kotlin.useTempFile
import xyz.xszq.otomadbot.mirai.reply

object Speech: CommandModule("语音功能", "speech") {
    val detector = LanguageDetectorBuilder.fromAllLanguages().build()
    val japaneseRegex = "([ぁ-んァ-ン])".toRegex()
    private val cooldown = Cooldown("speech")
    private val voiceDir = OtomadBotCore.configFolder.resolve("audio/otto").toVfs()
    private val tokensDir = voiceDir["tokens"]
    private val presetsDir = voiceDir["ysddTokens"]
    private fun asciiToPinyin(text: List<String>): List<String> {
        val result = text.toMutableList()
        result.forEachIndexed { index, t ->
            charTable.forEach { (char, py) ->
                if (char == t)
                    result[index] = py
            }
        }
        return result
    }
    override suspend fun subscribe() {
        events.subscribeGroupMessages {
            startsWith("活字印刷") { text ->
                voice.checkAndRun(this, listOf(text))
            }
        }
    }
    val voice = CommonCommandWithArgs("活字印刷", "tts") { args ->
        ifReady(cooldown) {
            val text = args?.firstOrNull() ?: return@ifReady
            if (text.isBlank() || text.length >= 120) {
                reply("使用方法：活字印刷 文本\n字数不能超过120个字！\n例：活字印刷 大家好啊，我是可怜Bot")
                return@ifReady
            }
            kotlin.runCatching {
                generate(text).useTempFile {
                    val silk = it.toSilk()
                    silk.toFile().toExternalResource().use { ex ->
                        subject.sendMessage((subject as AudioSupported).uploadAudio(ex))
                    }
                }
                cooldown.update(subject)
            }.onFailure {
                reply("生成失败，请检查内容是否为中文/英文/数字。")
            }
        }
    }
    suspend fun generate(text: String): VfsFile {
        var chars =
            asciiToPinyin(text.lowercase().filter {
                it.isLetter() || it.isDigit() || it.code in 0x4e00..0x9fff || it == '.'
            }.toPinyinList()).filter { it.isNotBlank() }.joinToString(",")
        SpeechConfig.data.presets.forEach { (id, names) ->
            names.forEach { name ->
                val pinyin = asciiToPinyin(name.toPinyinList()).joinToString(",")
                chars = chars.replace(pinyin, ",$id,")
            }
        }
        chars = chars.lowercase()
        val files = chars.split(",").filter { it.isNotBlank() }.mapNotNull {  id ->
            (tokensDir.list().firstOrNull {
                it.baseNameWithoutCompoundExtension == id
            } ?: presetsDir.list().firstOrNull {
                it.baseNameWithoutCompoundExtension == id
            }) ?.absolutePath
        }.take(120)
        return FFMpegTask(FFMpegFileType.MP3) {
            files.forEach { input(it) }
            filterComplex(List(files.size) { index -> "[$index:0]"}.joinToString("") +
                    "concat=n=${files.size}:v=0:a=1[out]")
            map("[out]")
        }.getResult()
    }
    private val charTable = buildMap {
        put("a", "EI")
        put("b", "BI")
        put("c", "XI")
        put("d", "DI")
        put("e", "YI")
        put("f", "AI,FU")
        put("g", "JI")
        put("h", "AI,CHI")
        put("i", "AI")
        put("j", "ZHEI")
        put("k", "KAI")
        put("l", "AI,LU")
        put("m", "AI,MU")
        put("n", "EN")
        put("o", "OU")
        put("p", "PI")
        put("q", "KIU")
        put("r", "A")
        put("s", "AI,SI")
        put("t", "TI")
        put("u", "YOU")
        put("v", "WEI")
        put("w", "DA,BU,LIU")
        put("x", "AI,KE,SI")
        put("y", "WAI")
        put("z", "ZEI")
        put(".", "DIAN")
        put("0", "LING")
        put("1", "YI")
        put("2", "ER")
        put("3", "SAN")
        put("4", "SI")
        put("5", "WU")
        put("6", "LIU")
        put("7", "QI")
        put("8", "BA")
        put("9", "JIU")
    }
}