package xyz.xszq.bot.meme

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.http.*
import korlibs.io.util.isDigit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import xyz.xszq.bot.*
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.ArgsNotEnoughException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.payload.MemeOption
import xyz.xszq.bot.payload.markdown.*
import xyz.xszq.bot.sekai.SekaiCharacter
import xyz.xszq.bot.sekai.SekaiSticker
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

@Suppress("unused")
class Meme: Plugin() {
    lateinit var config: MemeConfig
    lateinit var api: MemeAPI

    val sekai = SekaiSticker()
    val ba = BlueArchiveLogo()
    val fiveThousand = FiveThousandChoyen()
    val spherize = Spherize()
    val imSoHappy = ImSoHappy()
    val emojiKitchen = EmojiKitchen()

    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        ba.init()
        fiveThousand.init()
        sekai.init()

        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/meme.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<MemeConfig>()
        api = MemeAPI(config.server)
        runCatching {
            api.init()
        }.onFailure { e ->
            logger.warn { "警告：无法连接到 meme 服务器" }
        }

        setRoute()

        logger.info { "[表情包] 插件加载完成。" }
    }
    fun String.toKotlinRegex() = Regex(replace("(?P<", "(?<"))
    fun String.getGroups(): List<String> {
        val regex = """\(\?P<([^>]+)>""".toRegex()
        return regex.findAll(this)
            .map { it.groupValues[1] }
            .toList()
    }
    suspend fun setRoute() = route {
        startsWith("生成") { raw ->
            runCatching {
                meme(raw)
            }.onFailure { e ->
                memeErrorHandler(e)
            }
        }
        startsWith("pjsk") { raw ->
            runCatching {
                sekai(raw)
            }.onFailure { e ->
                sekaiErrorHandler(e)
            }
        }
        startsWith("ba") { raw ->
            runCatching {
                ba(raw)
            }.onFailure { e ->
                baErrorHandler(e)
            }
        }
        startsWith("5k") { raw ->
            runCatching {
                fiveThousand(raw)
            }.onFailure { e ->
                fiveThousandErrorHandler(e)
            }
        }
        startsWith("球面化") {
            runCatching {
                spherize()
            }.onFailure { e ->
                spherizeErrorHandler(e)
            }
        }
        startsWith("我巨爽") {
            runCatching {
                imSoHappy()
            }.onFailure { e ->
                imSoHappyErrorHandler(e)
            }
        }
        startsWith("表情合成") { raw ->
            emojiKitchen(raw)
        }
        always {
            emojiKitchen()
        }
    }
    val memeErrorHandler: ErrorHandler = { e ->
        when (e) {
            is ArgsNotEnoughException -> e.message ?.let { reply(it) }
            is NotFoundException -> reply(memeHelp)
            else -> e.printStackTrace()
        }
    }
    val sekaiErrorHandler: ErrorHandler = { e ->
        when (e) {
            is ArgsNotEnoughException -> {
                if (e.message == null || e.message ?.isBlank() == true)
                    selectSekaiCharacter()
                else
                    reply(e.message!!)
            }
            is NotFoundException -> reply(sekaiNotFound)
            else -> e.printStackTrace()
        }
    }
    val baErrorHandler: ErrorHandler = { e ->
        val help = buildString {
            appendLine( "这是一个生成蔚蓝档案LOGO风格文本的功能。" )
            appendLine( "使用方法：/ba 左侧文本 右侧文本" )
            appendLine( "\t例：/ba Blue Archive" )
            appendLine( "如果要生成的文本包含空格，请在两段文本中间换行" )
        }.trim().newLine()
        when (e) {
            is ArgsNotEnoughException -> reply(help)
            is NeedHelpException -> reply(help)
            else -> e.printStackTrace()
        }
    }
    val fiveThousandErrorHandler: ErrorHandler = { e ->
        val help = buildString {
            appendLine( "这是一个生成5000兆円风格文本的功能。" )
            appendLine( "使用方法：/5k 上方文本 下方文本" )
            appendLine( "\t例：/5k 5000兆円 欲しい！" )
            appendLine( "如果要生成的文本包含空格，请在两段文本中间换行" )
        }.trim().newLine()
        when (e) {
            is ArgsNotEnoughException -> reply(help)
            is NeedHelpException -> reply(help)
            else -> e.printStackTrace()
        }
    }
    val spherizeErrorHandler: ErrorHandler = { e ->
        val hint = when (this) {
            is GroupMessageEvent -> "手机端如想发送图片，请长按输入框，并点击“全屏输入”，再去相册选择图片即可。"
            else -> null
        }
        val help = buildString {
            appendLine( "使用方法：“@可怜BOT 球面化”并同时发送图片" )
            hint ?.let { appendLine(it) }
            appendLine()
            appendLine( "请点击下方选择图片：" )
        }.trim()
        when {
            e is ArgsNotEnoughException || e is NeedHelpException -> reply(
                Markdown(brief("球面化", help), Keyboard.create {
                    row { at("选择图片", "球面化", false, 1) }
                }))
            else -> e.printStackTrace()
        }
    }
    val imSoHappyErrorHandler: ErrorHandler = { e ->
        val hint = when (this) {
            is GroupMessageEvent -> "手机端如想发送图片，请长按输入框，并点击“全屏输入”，再去相册选择图片即可。"
            else -> null
        }
        val help = buildString {
            appendLine( "使用方法：“@可怜BOT 我巨爽”并同时发送图片" )
            hint ?.let { appendLine(it) }
            appendLine()
            appendLine( "请点击下方选择图片：" )
        }.trim()
        when {
            e is ArgsNotEnoughException || e is NeedHelpException -> reply(
                Markdown(brief("我巨爽", help), Keyboard.create {
                    row { at("选择图片", "我巨爽", false, 1) }
                }))
            else -> e.printStackTrace()
        }
    }
    val memeKeyboard = Keyboard.create {
        row {
            link("选择表情", "https://otmdb.cn/bot/meme/", id = "1")
            at("⚙ 生成表情", "\n", id = "2")
        }
    }
    val memeHelp = Markdown(
        brief("生成功能", buildString {
            appendLine( "这是一个生成表情包的功能。" )
            appendLine( "使用方法：@可怜BOT /生成 表情名称 参数" )
            appendLine( "手机端发送图片需长按输入框，点击“全屏输入”，再去相册勾选即可" )
            appendLine( "⬇请点击下方按钮选择表情：" )
        }),
        memeKeyboard
    )
    val sekaiKeyboard = Keyboard.create {
        row {
            link("选择表情", "https://otmdb.cn/bot/meme/pjsk", id = "1")
            at("⚙ 生成表情", "/pjsk ", id = "2")
        }
    }
    val sekaiNotFound = Markdown(
        brief("PJSK表情","该角色或图片不存在，请重新选择："),
        sekaiKeyboard
    )
    fun callKeyboard(
        name: String
    ) = Keyboard.create {
        row {
            at(name, name, id = "1")
        }
    }

    private suspend fun MessageEvent.meme(
        raw: String
    ) {
        val command = raw.split(" ").first()
        var texts = mutableListOf<String>()
        var images = listOf<Image>()
        var options = mutableMapOf<String, JsonPrimitive>()
        val matched = api.memes.values.firstOrNull {
            command in it.keywords
        } ?.let { matched ->
            val rawArgs = raw.substringAfter(command).trim().split(" ").toMutableList()
            matched.params.options.forEach { option ->
                rawArgs.parseOptions(option, options)
            }
            texts = rawArgs.take(matched.params.maxTexts).toMutableList()
            if (texts.size < matched.params.minTexts &&
                matched.params.defaultTexts.size >= matched.params.minTexts) {
                texts += matched.params.defaultTexts.subList(texts.size, matched.params.defaultTexts.size)
            }
            images = message.filterIsInstance<Image>().take(matched.params.maxImages)
            matched
        } ?: api.memes.values.firstNotNullOfOrNull { meme ->
            meme.shortcuts.firstNotNullOfOrNull { shortcut ->
                shortcut.pattern.toKotlinRegex().matchEntire(raw) ?.let {
                    Triple(meme, shortcut, it)
                }
            }
        } ?.let { (matched, shortcut, result) ->
            val groups = shortcut.pattern.getGroups()
            result.groupValues
                .subList(1, result.groupValues.size)
                .forEachIndexed { i, group ->
                    val name = "{${groups[i]}}"
                    if (name in shortcut.texts)
                        texts.add(group)
                }
            images = message.filterIsInstance<Image>().take(matched.params.maxImages)
            options = shortcut.options.mapValues { JsonPrimitive(it.value) }.toMutableMap()
            matched
        } ?: throw NotFoundException()
        if (texts.size < matched.params.minTexts)
            throw ArgsNotEnoughException("文本参数不足，需要${matched.params.minTexts}段文本作为参数")
        if (images.size < matched.params.minImages)
            throw ArgsNotEnoughException("图片数量不足，需要${matched.params.minImages}张图片")
        api.generate(
            key = matched.key,
            images = images,
            texts = texts,
            options = options
        ).send(this)
    }
    private suspend fun MessageEvent.sekai(
        raw: String
    ) {
        val args = raw.split(" ", limit = 2).filter { it.isNotBlank() }
        if (args.isEmpty())
            throw ArgsNotEnoughException()
        val name = args.first()
        val text = args.getOrNull(1)
        val (character, alias) = SekaiSticker.aliases.entries.firstNotNullOfOrNull { (character, aliases) ->
            aliases.firstOrNull { alias ->
                name.lowercase().startsWith(alias)
            } ?.let { Pair(character, it) }
        } ?: throw NotFoundException()
        val id = name.substringAfter(alias).filter { it.isDigit() }.toIntOrNull()
        val config = id ?.let {
            sekai.characters.firstOrNull {
                character == it.character && id == it.name.split(" ").last().toInt()
            }
        } ?: run {
            val options = sekai.characters.filter {
                character == it.character
            }
            selectSekaiImageId(character, options)
            return
        }
        text ?: throw ArgsNotEnoughException("请在点击图片编号后输入文本！\n使用方法：/pjsk 角色名+编号 要生成的文本")
        sekai.draw(config, text).use { it.encodePNG().send(this) }
    }
    private suspend fun MessageEvent.ba(
        raw: String
    ) {
        if (message.text.trim() == "/ba")
            throw NeedHelpException()
        if (message.text.trim().removePrefix("/").substringAfter("ba").firstOrNull()?.isWhitespace() == false)
            return
        var args = raw.trim().split(" ", limit = 2)
        when {
            "\n" in raw -> {
                args = raw.trim().split("\n", limit = 2)
                val textL = args[0].trim()
                val textR = args.getOrNull(1)?.trim() ?: throw ArgsNotEnoughException()
                ba.draw(textL, textR).use { it.encodePNG().send(this) }
            }
            args.size == 2 -> {
                val textL = args[0].trim()
                val textR = args[1].trim()
                ba.draw(textL, textR).use { it.encodePNG().send(this) }
            }
            else -> {
                throw ArgsNotEnoughException()
            }
        }
    }
    private suspend fun MessageEvent.fiveThousand(
        raw: String
    ) {
        var args = raw.trim().split(" ", limit = 2).filter { it.isNotBlank() }
        when {
            "\n" in raw -> {
                args = raw.trim().replace("\r", "\n").split("\n", limit = 2).filter {
                    it.isNotBlank()
                }
                val top = args.getOrNull(0)?.trim() ?: throw ArgsNotEnoughException()
                val bottom = args.getOrNull(1)?.trim()
                fiveThousand.draw(top, bottom).use { it.encodePNG().send(this) }
            }
            args.isNotEmpty() -> {
                val top = args[0].trim()
                val bottom = args.getOrNull(1)?.trim()
                fiveThousand.draw(top, bottom).use { it.encodePNG().send(this) }
            }
            else -> {
                if (message.text.trim() == "/5k")
                    throw NeedHelpException()
            }
        }
    }
    private suspend fun MessageEvent.spherize() {
        val image = message.filterIsInstance<Image>().firstOrNull() ?: throw ArgsNotEnoughException()
        spherize.handle(this, image.file)
    }
    private suspend fun MessageEvent.imSoHappy() {
        val image = message.filterIsInstance<Image>().firstOrNull() ?: throw ArgsNotEnoughException()
        imSoHappy.handle(this, image.file)
    }
    private suspend fun MessageEvent.emojiKitchen(
        raw: String
    ) {
        val list = if ("+" in raw)
            raw.split("+", limit = 2)
        else
            raw.splitEmojis()
        if (list.size != 2)
            return
        val (a, b) = list
        emojiKitchen(a, b)
    }
    private suspend fun MessageEvent.emojiKitchen() {
        val raw = text.trim()
        if ("+" !in raw)
            return
        val list = raw.split("+", limit = 2)
        if (list.size != 2)
            return
        val (a, b) = list
        emojiKitchen(a, b)
    }
    private suspend fun MessageEvent.emojiKitchen(
        a: String,
        b: String
    ) {
        emojiKitchen.mix(a, b) ?.let { file ->
            reply(Image(file))
        }
    }

    private fun MutableList<String>.parseOptions(
        option: MemeOption,
        options: MutableMap<String, JsonPrimitive>
    ) {
        if (option.parserFlags.short)
            findArg("-" + option.name.first(), option, options)
        if (option.parserFlags.long)
            findArg("--" + option.name, option, options)
        (option.parserFlags.shortAliases + option.parserFlags.longAliases).forEach { flag ->
            if (contains(flag)) {
                options[option.name] = JsonPrimitive(true)
                remove(flag)
            }
        }
    }
    private fun MutableList<String>.findArg(
        arg: String,
        option: MemeOption,
        options: MutableMap<String, JsonPrimitive>
    ) {
        if (contains(arg)) {
            val pos = indexOf(arg)
            if (option is MemeOption.BooleanOption) {
                options[option.name] = JsonPrimitive(true)
            } else {
                val value = get(pos + 1)
                removeAt(pos + 1)
                options[option.name] = when (option) {
                    is MemeOption.StringOption -> JsonPrimitive(value)
                    is MemeOption.IntegerOption -> JsonPrimitive(value.toInt())
                    is MemeOption.FloatOption -> JsonPrimitive(value.toFloat())
                    else -> throw Exception()
                }
            }
            removeAt(pos)
        }
    }
    private suspend fun ByteArray.send(
        event: MessageEvent
    ): Unit = useTempFile { file ->
        file.writeBytes(this)
        event.reply(Image(file))
    }

    private fun StringBuilder.buildTable(
        rows: List<List<String>>,
        cols: Int = 2
    ) {
        appendLine("|" + " |".repeat(cols))
        appendLine("|" + " :---: |".repeat(cols))
        rows.forEach { row ->
            append("|")
            row.forEach { cell ->
                append(cell)
                append("|")
            }
            repeat(cols - row.size) {
                append(" |")
            }
            appendLine()
        }
    }

    private suspend fun MessageEvent.selectSekaiCharacter() {
        val rows = SekaiSticker.aliases.map { (characterId, aliases) ->
            val displayName = aliases[1]
            val example = sekai.characters.first { it.character == characterId }
            val url = "https://static-1254441046.cos.ap-guangzhou.myqcloud.com/pjsk/${example.img.replace("png", "webp")}"
            val width = getImageWidth(example.img, 16.0)
            buildString {
                append("![preview #${width}px #16px]($url)")
                append(' ')
                val command = "/pjsk $characterId".encodeURLParameter()
                append("[$displayName](mqqapi://aio/inlinecmd?command=${command}&enter=true&reply=false)")
            }
        }.chunked(2)

        reply(Markdown(MarkdownData(buildString {
            appendLine("## 表情生成")
            appendLine( "> 这是一个生成初音未来：世界计划（プロセカ/pjsk）表情的功能。" )
            appendLine()
            appendLine( "⬇请点击角色名来选择想生成的角色：" )
            buildTable(rows)
        })))
    }

    private suspend fun MessageEvent.selectSekaiImageId(
        character: String,
        options: List<SekaiCharacter>
    ) {
        val rows = options.map { option ->
            val id = option.name.split(" ").last().toInt()
            val url = "https://static-1254441046.cos.ap-guangzhou.myqcloud.com/pjsk/${option.img.replace("png", "webp")}"
            val width = getImageWidth(option.img, 26.0)
            buildString {
                append("![preview #${width}px #26px]($url)")
                append(' ')
                val command = "/pjsk ${character}${id} ".encodeURLParameter()
                append("[#${id}](mqqapi://aio/inlinecmd?command=${command}&enter=false&reply=false)")
            }
        }.chunked(4)

        reply(Markdown(MarkdownData(buildString {
            appendLine("请点击要选择的图片编号并输入文本：")
            buildTable(rows, 4)
        })))
    }

    private suspend fun getImageWidth(
        path: String,
        height: Double = 50.0
    ): Int = withContext(Dispatchers.IO) {
        runCatching {
            SkiaImage.makeFromEncoded(File(sekai.imgDir, path).readBytes()).use { image ->
                (image.width * height / image.height)
                    .toInt()
                    .coerceAtLeast(1)
            }
        }.getOrDefault(height.toInt())
    }

    companion object {
        fun String.splitEmojis() = Regex("\\X").findAll(this).map { it.value }.toList()
        fun brief(
            title: String,
            content: String
        ) = MarkdownData("**$title**\n\n$content")
    }
}
