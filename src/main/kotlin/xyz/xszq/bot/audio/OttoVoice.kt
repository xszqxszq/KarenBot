package xyz.xszq.bot.audio

import korlibs.io.file.baseNameWithoutCompoundExtension
import korlibs.io.file.std.localCurrentDirVfs
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
        put("miyu", listOf("米浴说的道理", "啊米浴说的道理"))
        put("djha", listOf("大家好啊"))
        put("wssddl", listOf("我是说的道理"))
        put("jtlaidian", listOf("今天来点大家想看的东西", "今天来点儿大家想看的东西"))
        put("sddl", listOf("说的道理"))
        put("bobi", listOf("波比是我爹", "啊嘛波比是我爹"))
        put("aksa", listOf("奥克苏恩"))
        put("alaf", listOf("奥利安费", "奥力安费"))
        put("anxu", listOf("安修"))
        put("ands", listOf("欧内的手"))
        put("hbx", listOf("哈比下", "哈陛下", "哈比霞", "哈毙霞"))
        put("hh", listOf("好汉"))
        put("hlldxf", listOf("哈里路大旋风", "哈利路大旋风"))
        put("wow", listOf("哇袄", "哇奥", "沃袄"))
        put("hj", listOf("获嘉", "获加"))
        put("jb", listOf("击败", "鸡掰"))
        put("wsm", listOf("为什么"))
        put("wsmy", listOf("为什么耶", "为什么呀"))
        put("xg", listOf("炫狗"))
        put("zn", listOf("尊尼"))
        put("yzd", listOf("原子弹"))
        put("alala", listOf("奥利奥利安"))
        put("cc", listOf("冲刺"))
        put("hm", listOf("哈姆"))
        put("wuzi", listOf("乌兹"))
        put("yyds", listOf("永远的神"))
        put("yts", listOf("一坨屎", "一坨史", "一拖四", "一坨四", "依托史"))
        put("ytszg", listOf("一坨史这个"))
        put("sfrs", listOf("释放忍术"))
        put("wsfrs", listOf("我释放忍术"))
        put("nmyyq", listOf("纳米悠悠球"))
        put("yfsszz", listOf("影分身十字斩"))
        put("xwyt", listOf("吓我一跳我释放忍术"))
        put("wxhn", listOf("我喜欢你"))
        put("nxhw", listOf("你喜欢我"))
        put("xhn", listOf("喜欢你"))
        put("xhw", listOf("喜欢我"))
        put("xh", listOf("喜欢"))
    }
}