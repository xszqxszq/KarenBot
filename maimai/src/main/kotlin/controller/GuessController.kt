package xyz.xszq.bot.controller

import korlibs.io.file.VfsFile
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.vendors.currentDialect
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import xyz.xszq.bot.*
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.database.GuessGameStatus
import xyz.xszq.bot.database.GuessGameTable
import xyz.xszq.bot.database.MaimaiSettingsTable
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.music.MusicGenre
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.music.MusicType
import xyz.xszq.bot.payload.markdown.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Suppress("unused")
class GuessController(
    override val maimai: Maimai
): Controller(maimai) {
    private val hints = 6
    private val maxOpening = 8
    private val cooldown = 10000L
    private val subscribeId = ConcurrentHashMap<String, String>()
    private val eventToReply = ConcurrentHashMap<String, MessageEvent>()

    private val jacketUrl = maimai.config.tokens["assets-jacket"] ?: throw Exception("assets-jacket missing")

    override suspend fun setRoute() = maimai.route("/mai") {
        maimai.pluginLoader.bot.restoreGuessGame()

        startsWith("猜歌") {
            classical()
        }
        startsWith(listOf("舞萌开字母", "出你字母")) {
            opening()
        }
        startsWith("不玩了") {
            endGame(subscribeId[contextId])
        }
        startsWith(listOf("禁用猜歌", "禁止猜歌", "关闭猜歌")) {
            if (this is GroupMessageEvent)
                reply(showAdminPanel(disable = true))
        }
        startsWith(listOf("启用猜歌", "允许猜歌", "打开猜歌")) {
            if (this is GroupMessageEvent)
                reply(showAdminPanel(disable = false))
        }
        button("admin/guess") {
            val args = data.split(",")
            val disable = args[0].toInt() == 0
            val group = args[1]
            MaimaiSettingsTable[group, "guess"] = (args[0] == "1").toString()
            if (disable)
                reply("禁用猜歌成功，启用请@可怜BOT发送“启用猜歌”。")
            else
                reply("启用猜歌成功，启用请@可怜BOT发送“禁用猜歌”。")
        }
    }
    override suspend fun unload() {
        subscribeId.forEach { (_, id) ->
            maimai.pluginLoader.subscribes.stop(id)
        }
    }
    private suspend fun MessageEvent.playable(): Boolean {
        MaimaiSettingsTable[contextId, "guess"] ?.let {
            if (!it.toBoolean()) {
                reply("当前群猜歌已被禁用，若要启用请管理员@可怜BOT发送“启用猜歌”。")
                return false
            }
        }
        if (subscribeId.containsKey(contextId)) {
            reply("当前还有猜歌游戏正在进行中，回复机器人“不玩了”结束游戏")
            return false
        }
        return true
    }
    private suspend fun MessageEvent.save(
        status: GuessGameStatus,
    ): Unit = newSuspendedTransaction(Dispatchers.IO) {
        GuessGameTable.deleteWhere {
            GuessGameTable.id eq this@save.contextId
        }
        GuessGameTable.insert {
            it[GuessGameTable.id] = this@save.contextId
            it[GuessGameTable.eventType] = when (this@save) {
                is GroupMessageEvent -> "group"
                else -> "c2c"
            }
            it[GuessGameTable.eventId] = this@save.eventId
            it[GuessGameTable.messageId] = this@save.id
            it[GuessGameTable.senderId] = this@save.sender.id
            it[GuessGameTable.seq] = this@save.seq
            it[GuessGameTable.type] = when(status) {
                is GuessGameStatus.Classical -> "classical"
                is GuessGameStatus.Opening -> "opening"
            }
            it[GuessGameTable.status] = status
        }
    }
    private suspend fun MessageEvent.endGame(
        subscribesAt: String? = null
    ) = newSuspendedTransaction(Dispatchers.IO) {
        subscribesAt ?.let {
            bot.pluginLoader.subscribes.stop(subscribesAt)
        }
        subscribeId.remove(contextId)
        eventToReply.remove(contextId)
        GuessGameTable.deleteWhere {
            GuessGameTable.id eq contextId
        }
    }
    suspend fun Bot.restoreGuessGame() = newSuspendedTransaction(Dispatchers.IO) {
        GuessGameTable.selectAll().forEach { result ->
            val event = when (result[GuessGameTable.eventType]) {
                "group" -> GroupMessageEvent(
                    bot = this@restoreGuessGame,
                    eventId = result[GuessGameTable.eventId],
                    id = result[GuessGameTable.messageId],
                    message = MessageChain(),
                    sender = User(this@restoreGuessGame, result[GuessGameTable.senderId]),
                    group = Group(this@restoreGuessGame, result[GuessGameTable.id].value),
                    seq = result[GuessGameTable.seq]
                )
                else -> MessageEvent(
                    bot = this@restoreGuessGame,
                    eventId = result[GuessGameTable.eventId],
                    id = result[GuessGameTable.messageId],
                    message = MessageChain(),
                    sender = User(this@restoreGuessGame, result[GuessGameTable.senderId]),
                    seq = result[GuessGameTable.seq]
                )
            }
            if (!event.playable())
                return@forEach
            when (result[GuessGameTable.type]) {
                "classical" -> event.run {
                    val status = result[GuessGameTable.status] as GuessGameStatus.Classical
                    val music = maimai.music(status.musicId) ?: return@forEach
                    val descriptions = status.hints

                    val subscribesAt = UUID.randomUUID().toString()
                    subscribeId[event.contextId] = subscribesAt
                    eventToReply[event.contextId] = event

                    val hintJob = maimai.scope.launch {
                        hintClassical(event.contextId, subscribesAt, music, descriptions)
                    }

                    bot.pluginLoader.subscribes.always(subscribesAt) {
                        listenClassical(event.contextId, subscribesAt, music, hintJob)
                    }
                }
                "opening" -> event.run {
                    val status = result[GuessGameTable.status] as GuessGameStatus.Opening
                    val musics = status.musics.mapNotNull {
                        maimai.music(it.first) ?.let { music ->
                            Pair(music, it.second)
                        }
                    }.toMutableList()
                    val chars = status.opened.toMutableList()

                    val subscribesAt = UUID.randomUUID().toString()
                    subscribeId[event.contextId] = subscribesAt
                    eventToReply[event.contextId] = event

                    bot.pluginLoader.subscribes.always(subscribesAt) {
                        listenOpening(event.contextId, subscribesAt, musics, chars)
                    }

                }
                else -> GuessGameTable.deleteWhere {
                    GuessGameTable.id eq event.contextId
                }
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun MessageEvent.classical() {
        if (!playable())
            return

        val music = maimai.musics()
            .filter { it.genre != MusicGenre.Utage }
            .shuffled()
            .first()
        val options = hints + 1
        val descriptions = getDescriptions(music).shuffled().take(hints).mapIndexed { i, desc ->
            "提示${i + 1}/$options：这首歌$desc"
        }.toMutableList()

        val subscribesAt = UUID.randomUUID().toString()
        subscribeId[contextId] = subscribesAt
        eventToReply[contextId] = this

        save(GuessGameStatus.Classical(
            music.id,
            descriptions,
        ))

        maimai.logger.info { "当前正在猜测: ${music.id}. ${music.name}" }

        val hintJob = maimai.scope.launch {
            reply(buildString {
                appendLine()
                append( "这是一个 maimai 猜歌小游戏~" )
                appendLine()
                append( "你需要根据以下信息猜出以下是 maimai 中收录的哪一首歌。" )
                append( "可以@可怜BOT发送歌曲名称，说“不玩了”可以结束游戏哦~" )
                appendLine()
                append( "管理员可以通过@可怜BOT发送“禁用猜歌”来关闭猜歌" )
            })
            hintClassical(contextId, subscribesAt, music, descriptions)
        }

        bot.pluginLoader.subscribes.always(subscribesAt) {
            listenClassical(this@classical.contextId, subscribesAt, music, hintJob)
        }
    }
    private suspend fun MessageEvent.hintClassical(
        contextId: String,
        subscribesAt: String,
        music: MusicInfo,
        descriptions: List<String>?
    ) {
        descriptions ?.forEachIndexed { index, desc ->
            val hint = buildString {
                appendLine()
                appendLine(desc)
            }.trim()
            if (textMode())
                eventToReply[contextId] ?.reply(hint)
            else
                eventToReply[contextId] ?.reply(MarkdownTemplates.Templates.guess(hint))
            eventToReply[contextId] ?.save(GuessGameStatus.Classical(
                music.id,
                descriptions.subList(index + 1, descriptions.size),
            ))
            delay(cooldown)
        }

        descriptions ?.runCatching {
            val hint = "这首歌的封面部分如图，30秒后将揭晓答案哦~"
            val cropped = music.cover().randomSlice() ?: return@runCatching
            if (textMode()) {
                useTempFile(suffix = ".jpg") { file ->
                    file.writeBytes(cropped)
                    eventToReply[contextId] ?.reply(
                        hint.newLine().toPlainText() + Image(file)
                    )
                }
            } else {
                val uploaded = bot.cos.uploadBinary(cropped)
                eventToReply[contextId] ?.reply(MarkdownTemplates.Templates.guessCropped(
                    uploaded.url, hint)
                )
                maimai.scope.launch {
                    delay(10000L)
                    bot.cos.deleteFromCos(uploaded.filename)
                }
            }

        }
        delay(30000L)

        val hint = "很遗憾，没有人猜中哦".toPlainText() + music.infoText()
        if (textMode()) {
            eventToReply[contextId] ?.reply(hint)
        } else {
            val url = "$jacketUrl/${music.resourceId}.jpg"
            eventToReply[contextId] ?.reply(MarkdownTemplates.Templates.guessFinished(url, hint.text))
        }
        endGame(subscribesAt)
    }
    private suspend fun MessageEvent.listenClassical(
        contextId: String,
        subscribesAt: String,
        music: MusicInfo,
        hintJob: Job
    ) {
        if (contextId != this.contextId) {
            return
        }
        eventToReply[contextId] = this
        val input = text.trim()
        if (input.startsWith("不玩了")) {
            hintJob.cancel()
            endGame(subscribesAt)

            val hint = "游戏已结束。答案如下：".toPlainText() + music.infoText()
            if (textMode()) {
                reply(hint)
            } else {
                val url = "$jacketUrl/${music.resourceId}.jpg"
                reply(MarkdownTemplates.Templates.guessFinished(url, hint.text))
            }
            return
        }
        maimai.aliases.search(input).take(10).forEach { answer ->
            if (answer.name == music.name) {
                hintJob.cancel()
                endGame(subscribesAt)

                val hint = "恭喜你猜中了哦~".toPlainText() + music.infoText()
                if (textMode()) {
                    reply(hint)
                } else {
                    val url = "$jacketUrl/${music.resourceId}.jpg"
                    reply(MarkdownTemplates.Templates.guessFinished(url, hint.text))
                }
                return
            }
        }
    }
    private suspend fun MessageEvent.opening() {
        if (!playable())
            return

        val musics = maimai.musics()
            .filter { music -> music.genre != MusicGenre.Utage && (music.name.hasAlpha() || music.name.any { it.isDigit() }) }
            .shuffled()
            .take(maxOpening)
            .map { Pair(it, false) }
            .toMutableList()
        val chars = mutableListOf<Char>()

        val subscribesAt = UUID.randomUUID().toString()
        subscribeId[contextId] = subscribesAt
        eventToReply[contextId] = this

        save(GuessGameStatus.Opening(
            musics = musics.map { Pair(it.first.id, it.second) },
            opened = chars
        ))

        reply(buildString {
            appendLine()
            append( "这是一个 maimai 猜歌小游戏~" )
            appendLine()
            append( "你需要猜出八首来自 maimai 的歌曲曲名！" )
            append( "可以@可怜BOT说：“开字母”来尝试开一次字母，说“开歌”来直接开出歌曲，说“不玩了”可以结束游戏哦~" )
            appendLine()
            append( "管理员可以在游戏结束后@可怜BOT发送“禁用猜歌”来关闭猜歌功能" )
        })

        reply(showOpening(musics, chars))

        bot.pluginLoader.subscribes.always(subscribesAt) {
            listenOpening(this@opening.contextId, subscribesAt, musics, chars)
        }
    }
    private suspend fun MessageEvent.listenOpening(
        contextId: String,
        subscribesAt: String,
        musics: MutableList<Pair<MusicInfo, Boolean>>,
        chars: MutableList<Char>
    ) {
        if (contextId != this.contextId) {
            return
        }
        if (text.trim().startsWith("不玩了")) {
            reply(showOpening(musics, chars, true))
            endGame(subscribesAt)
            return
        }

        if (text.trim().startsWith("开字母")) {
            val char = text.trim().substringAfter("开字母").trim().firstOrNull() ?: return
            if (char in chars) {
                reply("字母“${char}”已经开过了！")
                return
            }
            chars.add(char.lowercase().toDBC().first())
            musics.forEachIndexed { index, (music, opened) ->
                if (!opened && music.name.all {
                        val ch = it.lowercase().toDBC().first()
                        ch in chars || ch.toString().isBlank()
                    })
                    musics[index] = Pair(music, true)
            }
        } else if (text.trim().startsWith("开歌")) {
            val name = text.trim().substringAfter("开歌").trim().lowercase().toDBC()
            val found = maimai.aliases.search(name).take(10)
            if (found.isEmpty()) {
                reply("歌曲不存在！")
                return
            }
            musics.firstOrNull { (music, _) -> found.any { it.name == music.name } } ?.let { item ->
                musics[musics.indexOf(item)] = Pair(item.first, true)
            } ?: run {
                reply("歌曲不在题目列表中！")
                return
            }
        } else {
            return
        }
        if (musics.all { (_, opened) -> opened }) {
            reply("恭喜您猜出了全部歌曲！")
            reply(showOpening(musics, chars, true))
            endGame(subscribesAt)
            return
        }
        save(GuessGameStatus.Opening(
            musics = musics.map { Pair(it.first.id, it.second) },
            opened = chars
        ))
        reply(showOpening(musics, chars))
    }
    private fun showOpening(
        musics: List<Pair<MusicInfo, Boolean>>,
        chars: List<Char>,
        all: Boolean = false
    ) = MarkdownData.create(MarkdownTemplates.GUESS) {
        "status" {
            "\uD83D\uDCA1已开出字母：${chars.joinToString(", ")}"
        }
        musics.forEachIndexed { index, (music, status) ->
            if (status) {
                "word${index+1}" {
                    "✅${music.name}"
                }
            } else {
                "word${index+1}" {
                    buildString {
                        if (all)
                            append("❌")
                        else
                            append("\uD83E\uDD14")
                        append(music.name.map { c ->
                            if (c.lowercase().toDBC().first() !in chars && c.toString().isNotBlank() && !all) '?' else c
                        }.joinToString(""))
                    }
                }
            }
        }
    }.toMessage(if (!all) getOpeningButtons() else MarkdownTemplates.Keyboards.GUESS_OPEN_AGAIN)

    private fun GroupMessageEvent.showAdminPanel(
        disable: Boolean
    ) = MarkdownData.create(MarkdownTemplates.BRIEF) {
        "title" {
            "猜歌设置"
        }
        "content" {
            "请管理员点击下方按钮确认" + (if (disable) "禁用" else "启用") + "猜歌："
        }
    }.toMessage(Keyboard.create {
        row {
            val display = if (disable) "⚠禁用猜歌" else "✅启用猜歌"
            button(
                id = "admin/guess",
                action = Action(
                    type = Action.CALLBACK,
                    data = (if (disable) "0" else "1") + ",${group.id}",
                    permission = Permission(Permission.OPERATORS)
                ),
                renderData = RenderData(
                    label = display,
                    visitedLabel = display,
                    style = RenderData.BLUE
                )
            )
        }
    })
    fun getOpeningButtons() = Keyboard.create {
        row {
            button(
                id = "1",
                action = Action(
                    type = Action.AT,
                    data = "开字母 ",
                    permission = Permission(Permission.EVERYONE)
                ),
                renderData = RenderData(
                    label = "\uD83D\uDD24开字母",
                    visitedLabel = "\uD83D\uDD24开字母",
                    style = RenderData.BLUE
                )
            )
            button(
                id = "2",
                action = Action(
                    type = Action.AT,
                    data = "开歌",
                    permission = Permission(Permission.EVERYONE)
                ),
                renderData = RenderData(
                    label = "\uD83C\uDFB6开歌",
                    visitedLabel = "\uD83C\uDFB6开歌",
                    style = RenderData.BLUE
                )
            )
            button(
                id = "3",
                action = Action(
                    type = Action.AT,
                    data = "不玩了",
                    permission = Permission(Permission.EVERYONE)
                ),
                renderData = RenderData(
                    label = "\uD83D\uDD1A不玩了",
                    visitedLabel = "\uD83D\uDD1A不玩了",
                    style = RenderData.GRAY
                )
            )
        }
    }

    private fun getDescriptions(song: MusicInfo) = listOf(
        listOf(
            "的版本为 ${song.version.name}${if (song.isNew) " (计入b15)" else ""}",
            if (song.type == MusicType.Deluxe) "是 DX 谱面"
            else if (maimai.music(song.id + 10000) != null) "既有 DX 谱面也有标准谱面" else "没有 DX 谱面"
        ),
        "的艺术家为 ${song.artist}",
        "的分类为 ${song.genre.genreName}",
        "的 BPM 为 ${song.bpm}",
        "的红谱等级为 ${song.charts[2].levelValue}",
        "的紫谱等级为 ${song.charts[3].levelValue}",
        "的紫谱谱师为 ${song.charts[3].notesDesigner}",
        "${if (song.charts.size == 4) "没有" else "有"}白谱"
    )
    private suspend fun VfsFile.randomSlice(size: Int = 66): ByteArray? = withContext(Dispatchers.IO) {
        val image = org.jetbrains.skia.Image.makeFromEncoded(readBytes())

        val maxX = (image.width - size).coerceAtLeast(0)
        val maxY = (image.height - size).coerceAtLeast(0)

        val x = if (maxX > 0) (0..maxX).random(Random(System.currentTimeMillis())) else 0
        val y = if (maxY > 0) (0..maxY).random(Random(System.currentTimeMillis())) else 0

        val surface = Surface.makeRasterN32Premul(size, size)

        val srcRect = Rect.makeXYWH(x.toFloat(), y.toFloat(), size.toFloat(), size.toFloat())
        val dstRect = Rect.makeXYWH(0f, 0f, size.toFloat(), size.toFloat())

        surface.canvas.drawImageRect(image, srcRect, dstRect)

        val croppedImage = surface.makeImageSnapshot()
        croppedImage.encodeToData(EncodedImageFormat.JPEG, 90) ?.bytes
    }
    private val MessageEvent.contextId
        get() = when(this) {
            is GroupMessageEvent -> group.id
            else -> sender.id
        }
}