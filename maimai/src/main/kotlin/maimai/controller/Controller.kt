package xyz.xszq.bot.maimai.controller

import korlibs.io.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.withTimeout
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.api.MaimaiAPI
import xyz.xszq.bot.maimai.component.MaimaiQuery
import xyz.xszq.bot.maimai.component.MarkdownTemplates
import xyz.xszq.bot.maimai.component.MarkdownTemplates.Templates.brief
import xyz.xszq.bot.maimai.component.WaitingEventData
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.exception.*
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.MusicInfo
import xyz.xszq.bot.maimai.music.UserQueryParams
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.newLine
import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.reply

sealed class Controller(
    open val maimai: Maimai
) {
    abstract suspend fun setRoute()
    open suspend fun unload() {}

    suspend fun MessageEvent.isAdmin(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        maimai.pluginLoader.subscribes.handle(ChannelEvent(
            bot = maimai.pluginLoader.bot,
            channelName = "admin-check",
            data = AdminCheckRequest(sender.id, deferred)
        ))
        return withTimeout(5000L) { deferred.await() }
    }

    suspend fun rhythm(block: suspend xyz.xszq.bot.subscribe.SubscribeBuilder.() -> Unit) {
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

    suspend fun handleError(event: MessageEvent, e: Throwable, user: UserQueryParams?) {
        with(event) {
            when (e) {
                is QQBindRequiredException -> messageUserNeedQQBind()
                is UserBindRequiredException -> messageUserNeedBind()
                is UserNotFoundException -> messageUserNotFound()
                is UserDeniedException -> user?.let { messageUserDenied(it) }
                is FilterNoResultException -> messageFilterNoResult()
                is FilterTooManyException -> messageFilterTooMany()
                is NoDataException -> messageNoData(e.api)
                is NotSupportedException -> messageNotSupported(e.message.orEmpty())
                is NotFoundException -> messageNotFound(e.message.orEmpty())
                is AuthorizationException -> messageNeedAuthorization()
                is UserOARequiredException -> requestOA()
                is IgnoreException -> {}
                else -> {
                    e.printStackTrace()
                    reply(MaimaiQuery.QUERY_FAILED)
                }
            }
        }
    }

    suspend fun MessageEvent.messageUserNeedQQBind() {
        maimai.messageToReplay[sender.id] = message.text.trim()
        if (textMode())
            reply(MaimaiQuery.NO_QQ_BINDINGS)
        else
            reply(brief("舞萌DX", "为了继续后续查询，请输入您的QQ号来绑定：").toMessage(Keyboard.create {
                row {
                    at("⬇点我输入", "/bind ", id = "1")
                }
            }))
    }
    suspend fun MessageEvent.messageUserNeedBind() {
        if (textMode())
            reply(MaimaiQuery.NO_BACKEND_BINDINGS)
        else
            reply(MarkdownTemplates.Templates.selectBackends(this.text))
    }
    suspend fun MessageEvent.messageUserNotFound() {
        reply(MaimaiQuery.USER_NOT_FOUND)
    }
    suspend fun MessageEvent.messageUserDenied(user: UserQueryParams) {
        if (user.isSelf) {
            if (textMode())
                reply(MaimaiQuery.USER_EULA)
            else
                reply(brief("舞萌DX", "请前往查分器同意用户协议再进行查询：").toMessage(Keyboard.create {
                    row {
                        link("前往查分器", "https://otmdb.cn/jump/maimaidxprober", id = "1")
                    }
                }))
        } else {
            reply(MaimaiQuery.USER_DENIED)
        }
    }
    suspend fun MessageEvent.messageFilterNoResult() {
        reply(MaimaiQuery.NO_RECORDS)
    }
    suspend fun MessageEvent.messageFilterTooMany() {
        reply(MaimaiQuery.TOO_MANY_RECORDS)
    }
    suspend fun MessageEvent.messageNoData(backend: MaimaiAPI) {
        if (textMode())
            reply(buildString {
                appendLine("您似乎尚未导入舞萌DX分数，请查看数据导入教程：")
                when (backend) {
                    is DivingFish -> appendLine("水鱼查分器：https://otmdb.cn/jump/maimaidxprober_import")
                    is LXNS -> appendLine("落雪查分器：https://otmdb.cn/jump/lxnsprober_import")
                }
            }.trim().newLine())
        else
            reply(brief("舞萌DX", "您似乎尚未导入舞萌DX分数到${backend.name}查分器，请参考下方教程：").toMessage(Keyboard.create {
                when (backend) {
                    is DivingFish -> row {
                        link("🐟水鱼(电脑/iOS)", "https://otmdb.cn/jump/maimaidxprober_import", id = "1")
                    }
                    is LXNS -> row {
                        link("❄落雪(电脑/手机)", "https://otmdb.cn/jump/lxnsprober_import", id = "2")
                    }
                }
                row {
                    link("🐇UsagiPass(iOS/安卓)", "https://otmdb.cn/jump/maimai_prober_mobile", id = "3")
                    link("🤖可怜BOT(安卓)", "https://bot-docs.otmdb.cn/maimai/update", id = "4")
                }
                row {
                    when (backend) {
                        is DivingFish -> row {
                            at("❄切换到落雪", "设置查分器 落雪", enter = true, id = "5")
                        }
                        is LXNS -> row {
                            at("🐟切换到水鱼", "设置查分器 水鱼", enter = true, id = "5")
                        }
                    }
                    at("切换到自动", "设置查分器 自动", enter = true, id = "6")
                }
            }))
    }
    suspend fun MessageEvent.messageNotSupported(message: String) {
        reply(message)
    }
    suspend fun MessageEvent.messageNotFound(message: String) {
        reply(message)
    }
    suspend fun MessageEvent.messageNeedAuthorization() {
        reply(MaimaiQuery.NEED_AUTHORIZATION)
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun MessageEvent.requestOA() {
        val token = UUID.randomUUID().toString()
        maimai.api.lxnsBindTokens[token] = WaitingEventData(this)
        val authUrl = "https://bot-api.otmdb.cn/jump/lxns-oa/$token"

        if (textMode()) {
            reply(buildString {
                appendLine("使用该功能时，需要您授权BOT访问您在落雪查分器的全部成绩信息。请您点击链接授权：")
                appendLine(authUrl)
            }.trim().newLine())
        } else {
            reply(Markdown(MarkdownData(buildString {
                appendLine("**请求授权**")
                appendLine()
                append("使用该功能时，需要您授权BOT访问您在落雪查分器的全部成绩信息，请点击下方登录并授权：")
            }), Keyboard.create {
                row { link("点我授权", authUrl) }
            }))
        }
    }

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
    suspend fun MessageEvent.queryByTextOrImage(
        text: String,
        helpText: String ?= null,
        action: suspend (String) -> Unit
    ) = when {
        text.isNotBlank() -> action(text.trim())
        reference != null -> with(maimai.query) {
            parseImage()
        }.forEach {
            action(it.title)
        }
        else -> helpText ?.let {
            reply(helpText)
        }
    }
}