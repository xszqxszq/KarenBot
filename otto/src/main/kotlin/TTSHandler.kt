package xyz.xszq.bot

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum
import com.github.houbb.pinyin.util.PinyinHelper
import com.github.medavox.ipa_transcribers.latin.English
import korlibs.io.file.VfsFile
import korlibs.io.file.baseNameWithoutCompoundExtension
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 * The handler for TTS Task.
 * @param config The config file.
 */
class TTSHandler(
    private val config: OttoConfig,
) {
    private val voiceDir = localCurrentDirVfs["data/audio/otto"]
    private val tokensDir = voiceDir["tokens"]
    private val presetsDir = voiceDir["ysddTokens"]
    private val ext = "wav"

    var tokens: List<String>
    var presets: List<String>

    /**
     * Initialize tokens and presets.
     */
    init {
        runBlocking {
            tokens = tokensDir.list().map { it.baseNameWithoutCompoundExtension }.toList()
            presets = presetsDir.list().map { it.baseNameWithoutCompoundExtension }.toList()
        }
    }

    /**
     * Generate TTS file for the text.
     * @param text The text to read.
     */
    suspend fun generate(text: String): VfsFile {
        val files = toPinyin(tokens, text).mapNotNull { word ->
            tokens.firstOrNull { token -> word == token } ?.let { token ->
                tokensDir["$token.$ext"]
            } ?: presets.firstOrNull { preset -> word == preset } ?.let { preset ->
                presetsDir["$preset.$ext"]
            }
        }.take(120)
        return newTempFile(suffix=".pcm").also { AudioHandler.mergeWaveFiles(files, it, true, 24000) }
    }

    /**
     * Convert a sentence to Pinyin.
     * @param tokens Available pinyin tokens.
     * @param text The text to process.
     */
    private fun toPinyin(tokens: List<String>, text: String): List<String> {
        var raw = text
        punctuations.forEach { punctuation ->
            raw.replace(punctuation, " ")
        }
        var words = chineseToPinyin(raw.lowercase().filter {
            it.isLetter() || it.isDigit() || it.code in 0x4e00..0x9fff || it == '.' || it == ' '
        }).filter { it.isNotBlank() }.joinToString(",")

        config.englishPresets.forEach { (id, names) ->
            names.forEach { name ->
                val pinyin = chineseToPinyin(name).joinToString(",")
                words = words.replace(pinyin, ",$id,".uppercase())
            }
        }

        config.presets.forEach { (id, names) ->
            names.forEach { name ->
                val pinyin = chineseToPinyin(name).joinToString(",")
                words = words.replace(pinyin, ",$id,".uppercase())
            }
        }

        words = words.split(",").flatMap { word ->
            if (word in tokens || word.all { it.isUpperCase() }) listOf(word)
            else englishWordToPinyin(word).mapNotNull { now ->
                if (now in tokens) now
                else charTable[now]
            }
        }.joinToString(",")
        return words.lowercase().split(",").filter { it.isNotBlank() }
    }

    /**
     * Convert Chinese characters to Pinyin.
     * @param text The text to convert.
     */
    private fun chineseToPinyin(text: String): List<String> {
        return PinyinHelper.toPinyin(text, PinyinStyleEnum.NORMAL).replace("ü", "v").split(" ")
    }

    /**
     * Convert English word to Pinyin through IPA.
     * @param word The word to convert.
     */
    private fun englishWordToPinyin(word: String): List<String> {
        var ipa = English.transcribe(word)

        convertTable.forEach { (before, after) ->
            ipa = ipa.replace(before, after)
        }

        var phonemes = mutableListOf<String>()
        var last = ""
        for (char in ipa) {
            if (last == "" || char.isDigit()) {
                last += char
                continue
            }
            if ((last.last() in consonants && char in vowels)
                || (last.last() == 't' && char == 'ʃ')
                || (last.last() in consonants && char == 'j')
                || (last.last() in vowels && char == 'n')) {
                last += char
            } else if (char == 'ː') {
                continue
            } else {
                phonemes.add(last)
                last = char.toString()
            }
        }
        if (last != "")
            phonemes.add(last)

        return phonemes.mapNotNull { it ->
            if (it.length == 1 && it[0] !in vowels && !it[0].isDigit()) consonantTable[it]
            else if (it in invalidTable) invalidTable[it]
            else it
        }
    }



    /**
     * Convert irregular characters in IPA to latin characters.
     */
    private val convertTable = buildMap {
        put("ʌ", "a")
        put("ɑ", "a")
        put("æ", "a")
        put("ɛ", "e")
        put("ə", "e")
        put("ɪ", "i")
        put("ɒ", "o")
        put("ʊ", "u")
        put("ɹ", "r")
        put("ɫ", "l")
        put("j", "y")
        put("v", "w")
        put("ʒ", "j")
        put("tʃ", "q")
        put("ʃ", "x")
        put("θ", "s")
    }

    /**
     * Vowels in IPA.
     */
    private val vowels = listOf(
        'a', 'e', 'i', 'o', 'u'
    )

    /**
     * Consonants in IPA.
     */
    private val consonants = listOf(
        'b', 'p', 'm', 'f',
        'd', 't', 'n', 'l',
        'g', 'k', 'h', 'w',
        'j', 'q', 'x',
        'z', 's', 'r', 'y',
    )

    /**
     * Consonant Table in IPA.
     */
    private val consonantTable = buildMap {
        put("b", "bu")
        put("p", "pu")
        put("m", "mu")
        put("f", "fu")
        put("d", "de")
        put("t", "te")
        put("n", "en")
        put("l", "er")
        put("g", "ge")
        put("k", "ke")
        put("h", "he")
        put("w", "wu")
        put("j", "ji")
        put("q", "qi")
        put("x", "xi")
        put("z", "zi")
        put("s", "si")
        put("r", "er")
        put("y", "yi")
    }

    /**
     * Convert invalid pronunciation in pinyin to valid table.
     */
    private val invalidTable = buildMap {
        put("be", "bei")
        put("pe", "pei")
        put("me", "mei")
        put("fe", "fei")
        put("do", "duo")
        put("to", "tuo")
        put("no", "nuo")
        put("lo", "luo")
        put("go", "gou")
        put("ko", "kou")
        put("ho", "hou")
        put("ja", "jia")
        put("je", "jie")
        put("jo", "jiu")
        put("qa", "qia")
        put("qe", "qie")
        put("qo", "qiu")
        put("xa", "xia")
        put("xe", "xie")
        put("xo", "xiu")
        put("zo", "zou")
        put("so", "sou")
        put("ra", "la")
        put("ro", "rou")
        put("yo", "you")
        put("i", "yi")
        put("o", "ou")
        put("u", "wu")
    }

    /**
     * Single character to pinyin table.
     */
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

    /**
     * Punctuations table.
     */
    private val punctuations = listOf(
        ",", "?", ";", ":", "\"", "'",
        "(", ")", "<", ">", "[", "]",
        "，", "、", "。", "?", "；",
        "“", "”", "‘", "’",
        "（", "）", "《", "》", "【", "】"
    )
}