package xyz.xszq.bot.voice

import com.hankcs.hanlp.HanLP
import korlibs.io.file.VfsFile
import korlibs.io.file.baseNameWithoutCompoundExtension
import kotlinx.coroutines.flow.toList
import marytts.LocalMaryInterface
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator
import xyz.xszq.bot.AudioHandler
import xyz.xszq.bot.newTempFile
import java.util.*

class TTSParser(
    val config: VoicePresets,
    voiceDir: VfsFile
) {
    private val tokensDir = voiceDir["tokens"]
    private val presetsDir = voiceDir["ysddTokens"]
    lateinit var tokens: Map<String, VfsFile>
    lateinit var presets: Map<String, VfsFile>
    lateinit var originalPresets: Map<String, VfsFile>
    lateinit var pinyinPresets: Map<String, VfsFile>
    private lateinit var charPresets: Map<Char, VfsFile>
    private lateinit var mary: LocalMaryInterface
    private lateinit var english: EnglishHandler
    private lateinit var kana: KanaHandler

    suspend fun init() {
        tokens = tokensDir.list().toList().associateBy {
            it.baseNameWithoutCompoundExtension
        }
        presets = presetsDir.list().toList().associateBy {
            it.baseNameWithoutCompoundExtension
        }
        originalPresets = buildMap {
            config.presets.forEach { (filename, aliases) ->
                val file = presets[filename] ?: return@forEach
                aliases.forEach { alias ->
                    putIfAbsent(alias, file)
                }
            }
        }.toList().sortedBy { -it.first.length }.toMap()
        pinyinPresets = buildMap {
            config.presets.forEach { (filename, aliases) ->
                val file = presets[filename] ?: return@forEach
                aliases.forEach { alias ->
                    val cleaned = alias.toPinyin()
                    putIfAbsent(cleaned, file)
                }
            }
        }.toList().sortedBy { -it.first.length }.toMap()
        charPresets = buildMap {
            put('.', "dian")
            put('0', "ling")
            put('1', "yi")
            put('2', "er")
            put('3', "san")
            put('4', "si")
            put('5', "wu")
            put('6', "liu")
            put('7', "qi")
            put('8', "ba")
            put('9', "jiu")
        }.mapNotNull { (char, name) ->
            tokens[name] ?.let {
                Pair(char, it)
            }
        }.toMap()

        // MaryTTS通过插件加载器的ClassLoader会有问题
        Configurator.setAllLevels(LogManager.getRootLogger().name, Level.ERROR)
        Configurator.setLevel("marytts", Level.ERROR)
        val pluginCl = this::class.java.classLoader
        val t = Thread.currentThread()
        val prev = t.contextClassLoader
        t.contextClassLoader = pluginCl
        mary = try {
            LocalMaryInterface().apply {
                locale = Locale.US
                outputType = "ALLOPHONES"
            }
        } finally {
            t.contextClassLoader = prev
        }

        english = EnglishHandler(mary)
        kana = KanaHandler(tokens)
    }

    private fun tokenize(
        text: String
    ): List<Token> {
        val result = mutableListOf<Token>()
        val now = StringBuilder()
        var nowType = ""
        fun flush() {
            if (now.isEmpty())
                return
            when (nowType) {
                "zh" -> result.add(Token.Raw.Chinese(now.toString()))
                "en" -> result.add(Token.Raw.English(now.toString()))
                "ja" -> result.add(Token.Raw.Japanese(now.toString()))
            }
            now.clear()
            nowType = ""
        }
        text.forEach { char ->
            when {
                char in charPresets.keys -> {
                    flush()
                    charPresets[char] ?.let { file ->
                        result.add(Token.Final.Char(file))
                    }
                }
                char.isChinese() -> {
                    if (now.isNotEmpty() && nowType != "zh")
                        flush()
                    nowType = "zh"
                    now.append(char)
                }
                char.isAlpha() -> {
                    if (now.isNotEmpty() && nowType != "en")
                        flush()
                    nowType = "en"
                    now.append(char)
                }
                char.isJapanese() -> {
                    if (now.isNotEmpty() && nowType != "ja")
                        flush()
                    nowType = "ja"
                    now.append(char)
                }
                else -> {
                    flush()
                }
            }
        }
        flush()
        return result
    }
    private fun clean(
        now: List<Token>
    ): List<Token> {
        val result = mutableListOf<Token>()
        now.forEach { token ->
            when (token) {
                is Token.Final -> {
                    result.add(token)
                }
                is Token.Raw.Chinese -> {
                    val pinyin = if (token.text.first() == '[') token.text else token.text.toPinyin()
                    val found = pinyinPresets.entries.firstOrNull {
                        pinyin.indexOf(it.key) >= 0
                    } ?: run {
                        pinyin.split(" ").forEach { char ->
                            tokens[char.unescape()] ?.let {
                                result.add(Token.Final.Char(it))
                            }
                        }
                        return@forEach
                    }

                    val (matched, file) = found
                    val index = pinyin.indexOf(matched)

                    val left = pinyin.substring(0, index).trim()
                    if (left.isNotEmpty())
                        result.add(Token.Raw.Chinese(left))

                    result.add(Token.Final.Preset(file))

                    val right = pinyin.substring(index + matched.length).trim()
                    if (right.isNotEmpty())
                        result.add(Token.Raw.Chinese(right))
                }
                is Token.Raw.English -> {
                    val found = config.englishPresets.entries.firstNotNullOfOrNull { (filename, aliases) ->
                        aliases.firstOrNull {
                            token.text.indexOf(it) >= 0
                        } ?.let { alias ->
                            Pair(alias, filename)
                        }
                    } ?: run {
                        english.convertWord(token.text.lowercase()).forEach { char ->
                            tokens[char] ?.let { file ->
                                result.add(Token.Final.Char(file))
                            }
                        }
                        return@forEach
                    }

                    val (matched, filename) = found
                    val index = token.text.indexOf(matched)

                    val left = token.text.substring(0, index).trim()
                    if (left.isNotEmpty())
                        result.add(Token.Raw.English(left))

                    presets[filename]?.let { file ->
                        result.add(Token.Final.Preset(file))
                    }

                    val right = token.text.substring(index + matched.length).trim()
                    if (right.isNotEmpty())
                        result.add(Token.Raw.English(right))
                }
                is Token.Raw.Japanese -> {
                    result.addAll(kana.parse(token.text))
                }
            }
        }
        return result
    }
    // TODO: Improve this temporary solution
    fun prepare(text: String): List<Token> {
        var texts: List<Token> = listOf(Token.Raw.Chinese(
            text.replace("\r", "").replace("\n", "").trim()
        ))
        while (true) {
            texts.filterIsInstance<Token.Raw.Chinese>()
                .firstOrNull { t -> originalPresets.any { it.key in t.text } } ?.let { t ->
                    val (name, file) = originalPresets.entries.first { it.key in t.text }

                    val index = texts.indexOf(t)

                    val before = t.text.substringBefore(name).let {
                        if (it.isEmpty())
                            listOf()
                        else
                            listOf(Token.Raw.Chinese(it))
                    }
                    val after = t.text.substringAfter(name).let {
                        if (it.isEmpty())
                            listOf()
                        else
                            listOf(Token.Raw.Chinese(it))
                    }

                    texts = texts.subList(0, index) + before + listOf(
                        Token.Final.Preset(file)
                    ) + after + texts.subList(index + 1, texts.size)
                } ?: break
        }
        return texts.flatMap { if (it is Token.Raw) tokenize(it.text) else listOf(it) }
    }
    fun parse(text: String): List<Token.Final> {
        var result = prepare(text)
        var counter = 0
        while (result.any { it is Token.Raw } && counter < 120) {
            result = clean(result)
            counter += 1
        }
        return result.filterIsInstance<Token.Final>()
    }


    private fun Char.isAlpha() =
        this in 'a'..'z' || this in 'A'..'Z'
    private fun Char.isChinese() =
        Character.UnicodeScript.of(this.code) == Character.UnicodeScript.HAN
    private fun Char.isJapanese() =
        Character.UnicodeScript.of(this.code) in listOf(
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HIRAGANA,
        )
    private fun String.toPinyin(): String {
        return HanLP.convertToPinyinString(this, " ", false)
            .split(" ").joinToString(" ") { "[$it]" }
    }
    private fun String.unescape() = substring(1, length - 1)


    /**
     * Generate TTS file for the text.
     * @param text The text to read.
     */
    suspend fun generate(text: String): VfsFile {
        val files = parse(text)
            .take(120)
            .map { it.file }
        return newTempFile(suffix = ".pcm").also {
            AudioHandler.mergeWaveFiles(files, it, true, 24000)
        }
    }
}