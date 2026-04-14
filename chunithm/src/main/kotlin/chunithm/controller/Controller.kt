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
import xyz.xszq.bot.chunithm.exception.AuthorizationException
import xyz.xszq.bot.chunithm.exception.NoDataException
import xyz.xszq.bot.chunithm.exception.QQBindRequiredException
import xyz.xszq.bot.chunithm.exception.UserBindRequiredException
import xyz.xszq.bot.chunithm.exception.UserDeniedException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.music.UserQueryParams
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
                is NoDataException -> messageNoData(e.api)
                is NotFoundException -> messageNotFound(e.message.orEmpty())
                is AuthorizationException -> messageNeedAuthorization()
                else -> reply(ChunithmQuery.QUERY_FAILED)
            }
        }
    }
    suspend fun MessageEvent.messageUserNeedQQBind() {
        if (textMode())
            reply(ChunithmQuery.NO_QQ_BINDINGS)
        else
            reply(brief("舞萌DX", "为了继续后续查询，请输入您的QQ号来绑定：").toMessage(Keyboard.create {
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
                reply(brief("舞萌DX", "请前往查分器同意用户协议再进行查询：").toMessage(Keyboard.create {
                    row {
                        link("前往查分器", "https://otmdb.cn/jump/maimaidxprober", id = "1")
                    }
                }))
        } else {
            reply(ChunithmQuery.USER_DENIED)
        }
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
            reply(brief("舞萌DX", "您似乎尚未导入舞萌DX分数到查分器，请参考下方教程：").toMessage(Keyboard.create {
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
            }))
    }
    suspend fun MessageEvent.messageNotFound(message: String) {
        reply(message)
    }
    suspend fun MessageEvent.messageNeedAuthorization() {
        reply(ChunithmQuery.NEED_AUTHORIZATION)
    }
}