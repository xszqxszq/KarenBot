package xyz.xszq.bot.audio

import com.soywiz.korio.file.baseNameWithoutCompoundExtension
import com.soywiz.korio.file.baseNameWithoutExtension
import com.soywiz.korio.file.std.localCurrentDirVfs
import kotlinx.coroutines.delay
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import xyz.xszq.bot.ffmpeg.FFMpegFileType
import xyz.xszq.bot.ffmpeg.FFMpegTask
import xyz.xszq.nereides.toPinyinList
import java.io.File

object OttoVoice {
    val voiceDir = localCurrentDirVfs["audio/otto"]
    val tokensDir = voiceDir["tokens"]
    val presetsDir = voiceDir["ysddTokens"]
    fun asciiToPinyin(text: String): String {
        var result = text.lowercase()
        charTable.forEach { (char, py) ->
            result = result.replace(char, ",$py,")
        }
        return result
    }
    suspend fun generate(text: String): File? {
        var chars =
            PinyinHelper.toHanYuPinyinString(
                asciiToPinyin(text.filter { it.isLetter() || it.isDigit() || it.code in 0x4e00..0x9fff }),
                HanyuPinyinOutputFormat().apply {
                    caseType = HanyuPinyinCaseType.LOWERCASE
                    toneType = HanyuPinyinToneType.WITHOUT_TONE
                    vCharType = HanyuPinyinVCharType.WITH_V
                },
                ",",
                true
            ).trim().split(",").filter { it.isNotBlank() }.joinToString(",") { it.lowercase() }
        presets.forEach { (id, names) ->
            names.forEach { name ->
                val pinyin = name.toPinyinList().joinToString(",")
                chars = chars.replace(pinyin, ",$id,")
            }
        }
        val files = chars.split(",").filter { it.isNotBlank() }.mapNotNull {  id ->
            (tokensDir.listRecursiveSimple() + presetsDir.listRecursiveSimple()).firstOrNull {
                it.baseNameWithoutCompoundExtension == id
            } ?.absolutePath
        }
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
    private val presets = buildMap {
        put("bobi", listOf("波比是我爹", "阿玛波比是我爹", "阿巴波比是我爹", "波比是我妈爹"))
        put("djha", listOf("大家好啊", "大家好"))
        put("jtld", listOf("今天来点大家想看的东西", "今天来点儿大家想看的东西"))
        put("miyu", listOf("啊米浴说的道理", "米浴说的道理"))
        put("wssddl", listOf("我是说的道理"))
        put("sddl", listOf("说的道理"))
        put("ydglm", listOf("一德格拉米"))
        put("by", listOf("白银"))
        put("ds", listOf("大司"))
        put("d", listOf("爹"))
        put("ga", listOf("滚啊", "滚"))
        put("snr", listOf("山泥若"))
        put("kzzll", listOf("卡在这里了"))
        put("jyxa", listOf("救一下啊", "救一下"))
        put("gwysmgx", listOf("跟我有什么关系"))
        put("nzmzmca", listOf("你怎么这么菜啊", "你怎么这么菜"))
        put("wao", listOf("哇袄", "哇奥"))
        put("omns", listOf("阿米诺斯", "阿弥诺斯"))
        put("wcsndm", listOf("我阐述你的梦"))
    }
}