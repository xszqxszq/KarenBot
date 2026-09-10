package xyz.xszq.bot.maimai.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import korlibs.io.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.event.ReplyAble
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.Maimai.Companion.textMode
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.api.MaimaiAPI
import xyz.xszq.bot.maimai.component.MaimaiQuery
import xyz.xszq.bot.maimai.component.MarkdownTemplates
import xyz.xszq.bot.maimai.component.WaitingEventData
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.ProberBindTable
import xyz.xszq.bot.maimai.database.QQBindTable
import xyz.xszq.bot.maimai.database.RhythmGameTokens
import xyz.xszq.bot.maimai.exception.*
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.MusicInfo
import xyz.xszq.bot.maimai.music.UserQueryParams
import xyz.xszq.bot.message.Image
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.RemoteImage
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.payload.markdown.MarkdownDsl
import xyz.xszq.bot.payload.markdown.RenderData
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.SubscribeBuilder

/**
 * 功能基类
 */
@Suppress("unused")
sealed class Controller(
    open val maimai: Maimai
) {
    val logger = KotlinLogging.logger {}
    /**
     * 注册命令路由
     */
    abstract suspend fun setRoute()
    /**
     * 插件卸载时执行
     */
    open suspend fun unload() {}

    /**
     * 消息发送者是否为管理员
     *
     * 这是一个远程调用，将会请求 admin 插件进行鉴权
     */
    suspend fun MessageEvent.isAdmin(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        maimai.pluginLoader.subscribes.handle(ChannelEvent(
            bot = maimai.pluginLoader.bot,
            channelName = "admin-check",
            data = AdminCheckRequest(sender.id, deferred)
        ))
        return runCatching { withTimeout(5000L) { deferred.await() } }.getOrDefault(false)
    }

    /**
     * 注册命令域
     *
     * 命令域下根据用户偏好选择唯一的插件处理其命令
     *
     * @param block 命令域路由代码块
     */
    suspend fun rhythm(block: suspend SubscribeBuilder.() -> Unit) {
        maimai.route("/mai") {
            domain(
                name = "rhythm",
                value = "maimai",
                defaultHandler = {
                    MaimaiSettingsTable.defaultGame(sender.id)
                },
                block = block
            )
        }
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复纯文本
     *
     * @param fallback 兼容模式时的回复
     * @param block Markdown DSL Builder
     */
    suspend fun MessageEvent.reply(
        fallback: String,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复纯文本
     *
     * @param fallback 兼容模式时的回复
     * @param markdown Markdown 内容
     */
    suspend fun MessageEvent.reply(fallback: String, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复消息链
     *
     * @param fallback 兼容模式时的消息链
     * @param block Markdown DSL Builder
     */
    suspend fun MessageEvent.reply(
        fallback: MessageChain,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复消息链
     *
     * @param fallback 兼容模式时的消息链
     * @param markdown Markdown 内容
     */
    suspend fun MessageEvent.reply(fallback: MessageChain, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复纯文本
     *
     * @param fallback 兼容模式时的回复
     * @param block Markdown DSL Builder
     */
    suspend fun ReplyAble.reply(
        fallback: String,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复纯文本
     *
     * @param fallback 兼容模式时的回复
     * @param markdown Markdown 内容
     */
    suspend fun ReplyAble.reply(fallback: String, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复消息链
     *
     * @param fallback 兼容模式时的消息链
     * @param block Markdown DSL Builder
     */
    suspend fun ReplyAble.reply(
        fallback: MessageChain,
        block: MarkdownDsl.() -> Unit
    ) {
        if (textMode()) reply(fallback)
        else reply(block)
    }

    /**
     * 回复用户消息
     *
     * 默认回复 Markdown 内容，兼容模式时回复消息链
     *
     * @param fallback 兼容模式时的消息链
     * @param markdown Markdown 内容
     */
    suspend fun ReplyAble.reply(fallback: MessageChain, markdown: Markdown) {
        if (textMode()) reply(fallback)
        else reply(markdown)
    }

    /**
     * 查询失败
     */
    suspend fun MessageEvent.messageQueryFailed() {
        reply(MaimaiQuery.QUERY_FAILED) {
            brief("查询失败", MaimaiQuery.QUERY_FAILED)
            keyboard {
                row {
                    at("🔄重试", text, enter = true)
                }
            }
        }
    }
    /**
     * 处理异常并回复
     *
     * @param event 消息事件
     * @param exception 查询异常
     * @param user 用户
     */
    suspend fun handleError(
        event: MessageEvent,
        exception: Throwable,
        user: UserQueryParams?
    ) {
        with(event) {
            when (exception) {
                is UserBindRequiredException -> {
                    val message = exception.message
                    if (message.isNullOrBlank())
                        messageUserNeedBind(true)
                    else
                        reply(message)
                }
                is UserQueriedNoBindingException -> reply(exception.message ?: "您查询的用户未绑定水鱼账户，无法查询")
                is UserNotFoundException -> messageUserNotFound()
                is UserDeniedException -> user?.let { messageUserDenied(it) }
                is FilterNoResultException -> messageFilterNoResult()
                is FilterTooManyException -> messageFilterTooMany()
                is NoDataException -> messageNoData(exception.api)
                is NotSupportedException -> messageNotSupported(exception.message.orEmpty())
                is NotFoundException -> messageNotFound(exception.message.orEmpty())
                is AuthorizationException -> messageNeedAuthorization()
                is IgnoreException -> {}
                else -> {
                    exception.printStackTrace()
                    messageQueryFailed()
                }
            }
        }
    }

    // TODO: Move URL to config
    private suspend fun MessageEvent.bindLinks(replay: Boolean = false): Pair<String, String> {
        val token = UUID.randomUUID().toString()
        val data = WaitingEventData(this, replay = replay)
        maimai.api.oauthBindTokens[token] = data
        RhythmGameTokens.save(token, this, replay, data.expireAt)
        val divingFishUrl = "https://bot-api.otmdb.cn/jump/diving-fish-oa/$token"
        val lxnsUrl = "https://bot-api.otmdb.cn/jump/lxns-oa/$token"
        return Pair(divingFishUrl, lxnsUrl)
    }
    /**
     * 提示用户绑定查分器账号
     *
     * @param replay 绑定完成后是否重放消息事件
     * @param fromBind 是否由绑定指令触发
     */
    suspend fun MessageEvent.messageUserNeedBind(
        replay: Boolean = false,
        fromBind: Boolean = false
    ) {
        val prefer = MaimaiSettingsTable[sender.id, "prober"]
        val migrateFailed = ProberBindTable[sender.id, "diving-fish", "migrate-failed"] != null
        val needReBind = !fromBind && migrateFailed &&
            (prefer.isNullOrBlank() || prefer == "diving-fish")
        val qq = QQBindTable[sender.id]
        val dfId = ProberBindTable[sender.id, "diving-fish", "id"]
        val dfUsername = ProberBindTable[sender.id, "diving-fish", "username"]
        val lxnsRefresh = ProberBindTable[sender.id, "lxns", "refresh"]
        val lxnsFriendCode = ProberBindTable[sender.id, "lxns", "friend-code"]
        logger.debug { "[绑定提示] $sender.id 查分器偏好=${prefer?.ifBlank { "自动" } ?: "自动"} QQ=${qq ?: "无"}" }
        logger.debug { "[绑定提示] 水鱼 id=${dfId ?: "无"} username=${dfUsername ?: "无"}" }
        logger.debug { "[绑定提示] 落雪 refresh=${lxnsRefresh ?: "无"} 好友码=${lxnsFriendCode ?: "无"}" }
        val (divingFishUrl, lxnsUrl) = bindLinks(replay = replay)
        val showSwitch = !fromBind && !prefer.isNullOrBlank()
        reply(buildString {
            appendLine("请根据您所使用的查分器，点击下面链接来绑定：")
            appendLine()
            appendLine("水鱼查分器：$divingFishUrl")
            appendLine("落雪查分器：$lxnsUrl")
            appendLine("如果您不知道什么是查分器，可以查看：https://bot-docs.otmdb.cn/maimai/prober")
        }.trim()) {
            brief("绑定查分器", when {
                needReBind -> "因水鱼查分器要求，需要您点击下方重新绑定账号，为此带来的不便十分抱歉"
                fromBind -> "使用前，请您点击下方来绑定您的舞萌/中二查分器账号："
                else -> "请选择您要使用的舞萌/中二查分器："
            })
            keyboard {
                when {
                    fromBind || prefer.isNullOrBlank() -> row {
                        link("🐟水鱼", divingFishUrl)
                        link("❄落雪", lxnsUrl)
                    }
                    prefer == "diving-fish" -> row {
                        link("🐟水鱼", divingFishUrl)
                    }
                    else -> row {
                        link("❄落雪", lxnsUrl)
                    }
                }
                if (showSwitch) {
                    row {
                        at("🔄切换查分器", "/bind", enter = true)
                    }
                } else {
                    row {
                        link("查分器是什么？", "https://bot-docs.otmdb.cn/maimai/prober", style = RenderData.GRAY)
                    }
                }
            }
        }
    }
    /**
     * 回复用户不存在的提示
     */
    suspend fun MessageEvent.messageUserNotFound() {
        reply(MaimaiQuery.USER_NOT_FOUND)
    }
    /**
     * 回复用户拒绝授权的提示
     *
     * @param user 被查询的用户
     */
    suspend fun MessageEvent.messageUserDenied(user: UserQueryParams) {
        if (user.isSelf) {
            reply(MaimaiQuery.USER_EULA) {
                brief("舞萌DX", "请前往查分器同意用户协议再进行查询：")
                keyboard {
                    row {
                        link("前往查分器", "https://otmdb.cn/jump/maimaidxprober")
                    }
                }
            }
        } else {
            reply(MaimaiQuery.USER_DENIED)
        }
    }
    /**
     * 回复筛选无结果的提示
     */
    suspend fun MessageEvent.messageFilterNoResult() {
        reply(MaimaiQuery.NO_RECORDS)
    }
    /**
     * 回复筛选结果过多的提示
     */
    suspend fun MessageEvent.messageFilterTooMany() {
        reply(MaimaiQuery.TOO_MANY_RECORDS)
    }
    /**
     * 回复尚无成绩数据的提示并附导入教程
     *
     * @param backend 查分器后端
     */
    suspend fun MessageEvent.messageNoData(backend: MaimaiAPI) {
        reply(buildString {
            appendLine("您似乎尚未导入舞萌DX分数，请查看数据导入教程：")
            when (backend) {
                is DivingFish -> appendLine("水鱼查分器：https://otmdb.cn/jump/maimaidxprober_import")
                is LXNS -> appendLine("落雪查分器：https://otmdb.cn/jump/lxnsprober_import")
            }
        }.trim().newLine()) {
            brief("舞萌DX", "您似乎尚未导入舞萌DX分数到${backend.name}查分器，请参考下方教程：")
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
                    link("\uD83E\uDDCABakapiano", "https://www.bilibili.com/video/BV1QdhM6REGX")
                    link("🐇UsagiPass", "https://otmdb.cn/jump/maimai_prober_mobile")
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
    /**
     * 回复不支持的操作提示
     *
     * @param message 提示文案
     */
    suspend fun MessageEvent.messageNotSupported(message: String) {
        reply(message)
    }
    /**
     * 回复查询内容不存在的提示
     *
     * @param message 提示文案
     */
    suspend fun MessageEvent.messageNotFound(message: String) {
        reply(message)
    }
    /**
     * 回复需查分器授权的提示
     */
    suspend fun MessageEvent.messageNeedAuthorization() {
        reply(MaimaiQuery.NEED_AUTHORIZATION)
    }

    /**
     * 使用命令时选择歌曲的菜单
     *
     * @param type 命令名
     * @param args 命令参数
     * @param needDifficulty 查询时是否可以带难度
     * @return 选中的曲目与难度
     */
    suspend fun MessageEvent.selectMusic(
        type: String,
        args: String,
        needDifficulty: Boolean
    ): Pair<MusicInfo, MusicDifficulty?>? {
        var difficulty = if (needDifficulty) MusicDifficulty.from(args.firstOrNull() ?.toString() ?: "") else null
        val name = difficulty ?.let { args.substring(1, args.length) } ?: args
        var result = maimai.aliases.search(name)
        if (difficulty != null)
            result = result.filter { it.charts.any { chart -> chart.difficulty == difficulty } }
        if (difficulty != null && result.isEmpty()) {
            difficulty = null
            result = maimai.aliases.search(args)
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
                        ))
            }
        }
        return null
    }
    /**
     * 根据文本或者图像进行查歌
     *
     * @param text 查询文本
     * @param helpText 帮助文本
     * @param action 查询后的代码块
     */
    suspend fun MessageEvent.queryByTextOrImage(
        text: String,
        helpText: String ?= null,
        action: suspend (String) -> Unit
    ) {
        when {
            text.isNotBlank() -> action(text.trim())
            reference != null || message.any { it is Image } -> {
                val client = maimai.pluginLoader.llmClient ?: return
                val images = (message.filterIsInstance<Image>().mapNotNull {
                    it.url.ifBlank { null }
                }) + (reference ?.filterIsInstance<RemoteImage>() ?.let {
                    it.map { image -> image.url }
                } ?: emptyList())
                if (images.isEmpty())
                    return
                maimai.query.parseImage(client, images).forEach {
                    action(it.title)
                }
            }
            else -> helpText ?.let {
                reply(helpText)
            }
        }
    }
}