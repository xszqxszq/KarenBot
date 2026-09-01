package xyz.xszq.bot.chunithm.controller

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import xyz.xszq.bot.chunithm.Chunithm
import xyz.xszq.bot.chunithm.Chunithm.Companion.textMode
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.api.DivingFish
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.component.ChunithmQuery
import xyz.xszq.bot.chunithm.component.ImageParseResult
import xyz.xszq.bot.chunithm.component.MarkdownTemplates
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.chunithm.exception.*
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.chunithm.music.UserQueryParams
import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.json
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.RemoteImage
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.payload.markdown.MarkdownDsl
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.SubscribeBuilder

@Suppress("unused")
sealed class Controller(
    open val chunithm: Chunithm
) {
    abstract suspend fun setRoute()
    open suspend fun unload() {}

    suspend fun MessageEvent.isAdmin(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        chunithm.pluginLoader.subscribes.handle(ChannelEvent(
            bot = chunithm.pluginLoader.bot,
            channelName = "admin-check",
            data = AdminCheckRequest(sender.id, deferred)
        ))
        return runCatching { withTimeout(5000L) { deferred.await() } }.getOrDefault(false)
    }

    suspend fun rhythm(
        block: suspend SubscribeBuilder.() -> Unit
    ) {
        chunithm.route("/chu") {
            domain(
                name = "rhythm",
                value = "chunithm",
                defaultHandler = {
                    MaimaiSettingsTable.defaultGame(sender.id)
                },
                block = block
            )
        }
    }

    suspend fun MessageEvent.reply(
        fallback: String,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    suspend fun MessageEvent.reply(fallback: String, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    suspend fun MessageEvent.reply(
        fallback: MessageChain,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    suspend fun MessageEvent.reply(fallback: MessageChain, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    suspend fun ReplyAble.reply(
        fallback: String,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    suspend fun ReplyAble.reply(fallback: String, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    suspend fun ReplyAble.reply(
        fallback: MessageChain,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    suspend fun ReplyAble.reply(fallback: MessageChain, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    suspend fun MessageEvent.messageQueryFailed() {
        reply(ChunithmQuery.QUERY_FAILED) {
            brief("查询失败", ChunithmQuery.QUERY_FAILED)
            keyboard {
                row {
                    at("🔄重试", text, enter = true)
                }
            }
        }
    }
    suspend fun handleError(
        event: MessageEvent,
        e: Throwable,
        user: UserQueryParams?
    ) {
        with(event) {
            when (e) {
                is UserBindRequiredException -> {
                    val message = e.message
                    if (message.isNullOrBlank()) {
                        val prefer = MaimaiSettingsTable[sender.id, "prober"]
                        val bound = when (prefer) {
                            "diving-fish" -> ProberBindTable[sender.id, "diving-fish", "id"] != null
                            "lxns" -> ProberBindTable[sender.id, "lxns", "refresh"] != null ||
                                    ProberBindTable[sender.id, "lxns", "friend-code"] != null
                            else -> ProberBindTable[sender.id, "diving-fish", "id"] != null ||
                                    ProberBindTable[sender.id, "lxns", "refresh"] != null ||
                                    ProberBindTable[sender.id, "lxns", "friend-code"] != null
                        }
                        if (bound)
                            messageQueryFailed()
                        else
                            messageUserNeedBind()
                    } else
                        reply(message)
                }
                is UserQueriedNoBindingException -> reply(e.message ?: "您查询的用户未绑定水鱼账户，无法查询")
                is UserNotFoundException -> messageUserNotFound()
                is UserDeniedException -> user?.let { messageUserDenied(it) }
                is FilterNoResultException -> messageFilterNoResult()
                is FilterTooManyException -> messageFilterTooMany()
                is NoDataException -> messageNoData(e.api)
                is NotSupportedException -> messageNotSupported(e.message.orEmpty())
                is NotFoundException -> messageNotFound(e.message.orEmpty())
                is AuthorizationException -> messageNeedAuthorization()
                is IgnoreException -> {}
                else -> {
                    e.printStackTrace()
                    messageQueryFailed()
                }
            }
        }
    }
    suspend fun MessageEvent.messageUserNeedBind() {
        requestOA()
    }
    suspend fun MessageEvent.messageUserNotFound() {
        reply(ChunithmQuery.USER_NOT_FOUND)
    }
    suspend fun MessageEvent.messageUserDenied(user: UserQueryParams) {
        if (user.isSelf) {
            reply(ChunithmQuery.USER_EULA) {
                brief("中二节奏", "请前往查分器同意用户协议再进行查询：")
                keyboard {
                    row {
                        link("前往查分器", "https://otmdb.cn/jump/maimaidxprober")
                    }
                }
            }
        } else {
            reply(ChunithmQuery.USER_DENIED)
        }
    }
    suspend fun MessageEvent.messageFilterNoResult() {
        reply(ChunithmQuery.NO_RECORDS)
    }
    suspend fun MessageEvent.messageFilterTooMany() {
        reply(ChunithmQuery.TOO_MANY_RECORDS)
    }
    suspend fun MessageEvent.messageNoData(backend: ChunithmAPI) {
        reply(buildString {
            appendLine("您似乎尚未导入中二节奏分数，请查看数据导入教程：")
            when (backend) {
                is DivingFish -> appendLine("水鱼查分器：https://otmdb.cn/jump/maimaidxprober_import")
                is LXNS -> appendLine("落雪查分器：https://otmdb.cn/jump/lxnsprober_import")
            }
        }.trim().newLine()) {
            brief("中二节奏", "您似乎尚未导入中二节奏分数到${backend.name}查分器，请参考下方教程：")
            keyboard {
                when (backend) {
                    is DivingFish -> row {
                        link("🐟水鱼(电脑/iOS)", "https://otmdb.cn/jump/maimaidxprober_import")
                    }
                    is LXNS -> row {
                        link("❄落雪(电脑/手机)", "https://otmdb.cn/jump/lxnsprober_import")
                    }
                }
                row {
                    when (backend) {
                        is DivingFish -> row {
                            at("❄切换到落雪", "设置查分器 落雪", enter = true)
                        }
                        is LXNS -> row {
                            at("🐟切换到水鱼", "设置查分器 水鱼", enter = true)
                        }
                    }
                    at("切换到自动", "设置查分器 自动", enter = true)
                }
            }
        }
    }
    suspend fun MessageEvent.messageNotSupported(message: String) {
        reply(message)
    }
    suspend fun MessageEvent.messageNotFound(message: String) {
        reply(message)
    }
    suspend fun MessageEvent.messageNeedAuthorization() {
        reply(ChunithmQuery.NEED_AUTHORIZATION)
    }

    suspend fun MessageEvent.requestOA() {
        bot.pluginLoader.subscribes.handle(ChannelEvent(bot, channelName = "rhythm-game-bind", data = this))
    }

    suspend fun MessageEvent.queryByTextOrImage(
        text: String,
        helpText: String? = null,
        action: suspend (String) -> Unit
    ) {
        if (text.isNotBlank()) {
            action(text.trim())
            return
        }
        val images = reference?.filterIsInstance<RemoteImage>() ?: emptyList()
        if (images.isEmpty()) {
            helpText?.let { reply(it) }
            return
        }
        val deferred = CompletableDeferred<String>()
        chunithm.pluginLoader.subscribes.handle(ChannelEvent(
            bot = chunithm.pluginLoader.bot,
            channelName = "parse-image",
            data = json.encodeToString(images.map { it.url }) to deferred
        ))
        val results = json.decodeFromString<List<ImageParseResult>>(
            withTimeout(60000L) { deferred.await() }
        )
        results.forEach { action(it.title) }
    }

    suspend fun MessageEvent.selectMusic(
        type: String,
        args: String,
        needDifficulty: Boolean
    ): Pair<MusicInfo, MusicDifficulty?>? {
        var difficulty = if (needDifficulty) MusicDifficulty.from(args.firstOrNull() ?.toString() ?: "") else null
        val name = difficulty ?.let { args.substring(1, args.length) } ?: args
        var result = chunithm.aliases.search(name)
        if (difficulty != null)
            result = result.filter { it.charts.any { chart -> chart.difficulty == difficulty } }
        if (difficulty != null && result.isEmpty()) {
            difficulty = null
            result = chunithm.aliases.search(args)
        }
        when (result.size) {
            0 -> throw NotFoundException("未找到该歌曲")
            1 -> return Pair(result.first(), difficulty)
            else -> {
                if (textMode())
                    return Pair(result.first(), difficulty)
                else
                    reply(
                        MarkdownTemplates.Templates.selectMusic(
                            title = "您要查找的歌曲可能是：",
                            type = type,
                            keyword = args,
                            difficulty = difficulty,
                            result = result
                        )
                    )
            }
        }
        return null
    }
}