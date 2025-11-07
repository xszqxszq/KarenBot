package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.image.bitmap.Bitmap
import korlibs.image.format.PNG
import korlibs.image.format.encode
import korlibs.image.format.readNativeImage
import korlibs.io.util.isDigit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.ArgsNotEnoughException
import xyz.xszq.bot.exception.NeedHelpException
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.payload.MemeOption
import xyz.xszq.bot.payload.markdown.Action
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.Permission
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.sekai.SekaiSticker
import kotlin.collections.plus
import kotlin.collections.set

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
    override fun load() {
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/meme.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<MemeConfig>()
        api = MemeAPI(config.server)

        runBlocking {
            api.init()
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
    fun setRoute() = route {
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
            is ArgsNotEnoughException -> reply(sekaiHelp)
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
        val help = buildString {
            appendLine( "使用方法：“@可怜BOT 球面化”并同时发送图片" )
            appendLine( "手机端发送方式：长按输入框，点击全屏输入，再去相册勾选即可" )
            appendLine()
            appendLine( "请发送想要球面化的图片" )
        }.trim()
        when {
            e is ArgsNotEnoughException || e is NeedHelpException -> reply(MarkdownData.create(BRIEF) {
                "title" {
                    "球面化"
                }
                "content" {
                    help.replace("\n", "\r")
                }
            }.toMessage(callKeyboard("球面化")))
            else -> e.printStackTrace()
        }
    }
    val imSoHappyErrorHandler: ErrorHandler = { e ->
        val help = buildString {
            appendLine( "使用方法：“@可怜BOT 我巨爽”并同时发送图片" )
            appendLine( "手机端发送方式：长按输入框，点击全屏输入，再去相册勾选即可" )
            appendLine()
            appendLine( "请发送想要我巨爽的图片" )
        }.trim()
        when {
            e is ArgsNotEnoughException || e is NeedHelpException -> reply(MarkdownData.create(BRIEF) {
                "title" {
                    "我巨爽"
                }
                "content" {
                    help.replace("\n", "\r")
                }
            }.toMessage(callKeyboard("我巨爽")))
            else -> e.printStackTrace()
        }
    }
    val memeKeyboard = Keyboard.create {
        row {
            button(
                id = "1",
                renderData = RenderData(
                    label = "选择表情",
                    visitedLabel = "选择表情",
                    style = RenderData.BLUE
                ),
                action = Action(
                    type = Action.LINK,
                    permission = Permission(Permission.EVERYONE),
                    data = "https://otmdb.cn/bot/meme/"
                )
            )
            button(
                id = "2",
                renderData = RenderData(
                    label = "⚙ 生成表情",
                    visitedLabel = "⚙ 生成表情",
                    style = RenderData.BLUE
                ),
                action = Action(
                    type = Action.AT,
                    permission = Permission(Permission.EVERYONE),
                    data = "\n"
                )
            )
        }
    }
    val memeHelp = MarkdownData.create(BRIEF) {
        "title" {
            "生成功能"
        }
        "content" {
            buildString {
                appendLine( "这是一个生成表情包的功能。" )
                appendLine( "使用方法：@可怜BOT /生成 表情名称 参数" )
                appendLine( "手机端发送图片需长按输入框，点击“全屏输入”，再去相册勾选即可" )
                appendLine( "⬇请点击下方按钮选择表情：" )
            }.trim().replace("\n", "\r")
        }
    }.toMessage(memeKeyboard)
    val sekaiKeyboard = Keyboard.create {
        row {
            button(
                id = "1",
                renderData = RenderData(
                    label = "选择表情",
                    visitedLabel = "选择表情",
                    style = RenderData.BLUE
                ),
                action = Action(
                    type = Action.LINK,
                    permission = Permission(Permission.EVERYONE),
                    data = "https://otmdb.cn/bot/meme/pjsk"
                )
            )
            button(
                id = "2",
                renderData = RenderData(
                    label = "⚙ 生成表情",
                    visitedLabel = "⚙ 生成表情",
                    style = RenderData.BLUE
                ),
                action = Action(
                    type = Action.AT,
                    permission = Permission(Permission.EVERYONE),
                    data = "/pjsk "
                )
            )
        }
    }
    val sekaiHelp = MarkdownData.create(BRIEF) {
        "title" {
            "PJSK表情"
        }
        "content" {
            buildString {
                appendLine( "这是一个生成PJSK（プロセカ）表情包的功能。" )
                appendLine( "使用方法：/pjsk 角色名+图片编号 文本" )
                appendLine( "\uD83D\uDC49\uFE0F/pjsk 初音6 已举办" )
                appendLine( "\uD83D\uDC49\uFE0F/pjsk ena9 喜欢你" )
                appendLine( "\uD83D\uDC49\uFE0F/pjsk mzk 这是随机" )
                appendLine( " " )
                appendLine( "⬇请点击下方按钮选择表情：" )
            }.trim().replace("\n", "\r")
        }
    }.toMessage(sekaiKeyboard)
    val sekaiNotFound = MarkdownData.create(BRIEF) {
        "title" {
            "PJSK表情"
        }
        "content" {
            "该角色或图片不存在，请重新选择："
        }
    }.toMessage(sekaiKeyboard)
    fun callKeyboard(
        name: String
    ) = Keyboard.create {
        row {
            button(
                id = "1",
                renderData = RenderData(
                    label = name,
                    visitedLabel = name,
                    style = RenderData.BLUE
                ),
                action = Action(
                    type = Action.AT,
                    permission = Permission(Permission.EVERYONE),
                    data = name
                )
            )
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
        val args = raw.split(" ", limit = 2)
        if (args.size != 2)
            throw ArgsNotEnoughException()
        val (name, text) = args
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
            sekai.characters.filter {
                character == it.character
            }.randomOrNull()
        } ?: throw NotFoundException()
        sekai.draw(config, text).encode(PNG).send(this)
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
                ba.draw(textL, textR).encode(PNG).send(this)
            }
            args.size == 2 -> {
                val textL = args[0].trim()
                val textR = args[1].trim()
                ba.draw(textL, textR).encode(PNG).send(this)
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
                args = raw.trim().split("\n", limit = 2).filter { it.isNotBlank() }
                val top = args.getOrNull(0)?.trim() ?: throw ArgsNotEnoughException()
                val bottom = args.getOrNull(1)?.trim()
                fiveThousand.draw(top, bottom).encode(PNG).send(this)
            }
            args.isNotEmpty() -> {
                val top = args[0].trim()
                val bottom = args.getOrNull(1)?.trim()
                fiveThousand.draw(top, bottom).encode(PNG).send(this)
            }
            else -> {
                if (message.text.trim() == "/5k")
                    throw NeedHelpException()
            }
        }
    }
    private suspend fun MessageEvent.spherize() {
        val image = message.filterIsInstance<Image>().firstOrNull() ?: throw ArgsNotEnoughException()
        spherize.handle(this, image.file.readNativeImage())
    }
    private suspend fun MessageEvent.imSoHappy() {
        val image = message.filterIsInstance<Image>().firstOrNull() ?: throw ArgsNotEnoughException()
        imSoHappy.handle(this, image.file.readNativeImage())
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

    companion object {
        const val BRIEF = "102112100_1761189409"
        fun String.splitEmojis() = Regex("\\X").findAll(this).map { it.value }.toList()
    }
}