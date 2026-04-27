package xyz.xszq.bot.chunithm.controller

import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.Chunithm.Companion.textMode
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.api.DivingFish
import xyz.xszq.bot.chunithm.api.LXNS
import xyz.xszq.bot.chunithm.component.ChunithmQuery
import xyz.xszq.bot.chunithm.component.MarkdownTemplates
import xyz.xszq.bot.chunithm.component.MarkdownTemplates.Templates.brief
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.exception.*
import xyz.xszq.bot.chunithm.music.UserQueryParams
import xyz.xszq.bot.event.ChannelEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.newLine
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.reply
import xyz.xszq.bot.subscribe.SubscribeBuilder

sealed class Controller(
    open val chunithm: Chunithm
) {
    abstract suspend fun setRoute()
    open suspend fun unload() {}

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

    suspend fun handleError(
        event: MessageEvent,
        e: Throwable,
        user: UserQueryParams?
    ) {
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
                else -> reply(ChunithmQuery.QUERY_FAILED)
            }
        }
    }
    suspend fun MessageEvent.messageUserNeedQQBind() {
        if (textMode())
            reply(ChunithmQuery.NO_QQ_BINDINGS)
        else
            reply(brief("中二节奏", "为了继续后续查询，请输入您的QQ号来绑定：").toMessage(Keyboard.create {
                row {
                    at("⬇点我输入", "/bind ", id = "1")
                }
            }))
    }
    suspend fun MessageEvent.messageUserNeedBind() {
        if (textMode())
            reply(ChunithmQuery.NO_BACKEND_BINDINGS)
        else
            reply(MarkdownTemplates.Templates.selectBackends(this.text))
    }
    suspend fun MessageEvent.messageUserNotFound() {
        reply(ChunithmQuery.USER_NOT_FOUND)
    }
    suspend fun MessageEvent.messageUserDenied(user: UserQueryParams) {
        if (user.isSelf) {
            if (textMode())
                reply(ChunithmQuery.USER_EULA)
            else
                reply(brief("中二节奏", "请前往查分器同意用户协议再进行查询：").toMessage(Keyboard.create {
                    row {
                        link("前往查分器", "https://otmdb.cn/jump/maimaidxprober", id = "1")
                    }
                }))
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
        if (textMode())
            reply(buildString {
                appendLine("您似乎尚未导入中二节奏分数，请查看数据导入教程：")
                when (backend) {
                    is DivingFish -> appendLine("水鱼查分器：https://otmdb.cn/jump/maimaidxprober_import")
                    is LXNS -> appendLine("落雪查分器：https://otmdb.cn/jump/lxnsprober_import")
                }
            }.trim().newLine())
        else
            reply(brief("中二节奏", "您似乎尚未导入中二节奏分数到${backend.name}查分器，请参考下方教程：").toMessage(Keyboard.create {
                when (backend) {
                    is DivingFish -> row {
                        link("🐟水鱼(电脑/iOS)", "https://otmdb.cn/jump/maimaidxprober_import", id = "1")
                    }
                    is LXNS -> row {
                        link("❄落雪(电脑/手机)", "https://otmdb.cn/jump/lxnsprober_import", id = "2")
                    }
                }
                row {
                    when (backend) {
                        is DivingFish -> row {
                            at("❄切换到落雪", "设置查分器 落雪", enter = true, id = "3")
                        }
                        is LXNS -> row {
                            at("🐟切换到水鱼", "设置查分器 水鱼", enter = true, id = "3")
                        }
                    }
                    at("切换到自动", "设置查分器 自动", enter = true, id = "4")
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
        reply(ChunithmQuery.NEED_AUTHORIZATION)
    }

    suspend fun MessageEvent.requestOA() {
        bot.pluginLoader.subscribes.handle(ChannelEvent(bot, channelName = "lxns-oa", data = this))
    }
}