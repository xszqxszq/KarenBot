package xyz.xszq.bot.controller

import korlibs.io.util.UUID
import kotlinx.coroutines.DelicateCoroutinesApi
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.api.DivingFish
import xyz.xszq.bot.api.LXNS
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.component.MaimaiQuery
import xyz.xszq.bot.component.MarkdownTemplates
import xyz.xszq.bot.component.WaitingEventData
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.*
import xyz.xszq.bot.music.UserQueryParams
import xyz.xszq.bot.newLine
import xyz.xszq.bot.reply

sealed class Controller(
    open val maimai: Maimai
) {
    abstract suspend fun setRoute()
    open suspend fun unload() {}

    suspend fun handleError(event: MessageEvent, e: Throwable, user: UserQueryParams?) {
        with(event) {
            when (e) {
                is QQBindRequiredException -> messageUserNeedQQBind()
                is UserBindRequiredException -> messageUserNeedBind()
                is UserNotFoundException -> messageUserNotFound()
                is UserDeniedException -> user?.let { messageUserDenied(it) }
                is FilterNoResultException -> messageFilterNoResult()
                is NoDataException -> messageNoData(e.api)
                is NotSupportedException -> messageNotSupported(e.message.orEmpty())
                is NotFoundException -> messageNotFound(e.message.orEmpty())
                is AuthorizationException -> messageNeedAuthorization()
                is UserOARequiredException -> requestOA()
                else -> reply(MaimaiQuery.QUERY_FAILED)
            }
        }
    }

    suspend fun MessageEvent.messageUserNeedQQBind() {
        if (textMode())
            reply(MaimaiQuery.NO_QQ_BINDINGS)
        else
            reply(MarkdownTemplates.Templates.BIND_QQ)
    }
    suspend fun MessageEvent.messageUserNeedBind() {
        if (textMode())
            reply(MaimaiQuery.NO_BACKEND_BINDINGS)
        else
            reply(MarkdownTemplates.Templates.SELECT_BACKENDS)
    }
    suspend fun MessageEvent.messageUserNotFound() {
        reply(MaimaiQuery.USER_NOT_FOUND)
    }
    suspend fun MessageEvent.messageUserDenied(user: UserQueryParams) {
        if (user.isSelf) {
            if (textMode())
                reply(MaimaiQuery.USER_EULA)
            else
                reply(MarkdownTemplates.Templates.USER_EULA)
        } else {
            reply(MaimaiQuery.USER_DENIED)
        }
    }
    suspend fun MessageEvent.messageFilterNoResult() {
        reply(MaimaiQuery.NO_RECORDS)
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
        else reply(MarkdownTemplates.Templates.importData(backend))
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
        maimai.api.bindTokens[token] = WaitingEventData(this)
        val authUrl = "https://bot-api.otmdb.cn/jump/lxns-oa/$token"

        if (textMode()) {
            reply(buildString {
                appendLine("使用该功能时，需要您授权BOT访问您在落雪查分器的全部成绩信息。请您点击链接授权：")
                appendLine(authUrl)
            }.trim().newLine())
        } else {
            reply(MarkdownTemplates.Templates.oauth(authUrl))
        }
    }
}