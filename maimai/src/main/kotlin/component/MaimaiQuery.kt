package xyz.xszq.bot.component

import korlibs.io.util.UUID
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.Maimai.Companion.textMode
import xyz.xszq.bot.MarkdownTemplates
import xyz.xszq.bot.api.MaimaiAPI
import xyz.xszq.bot.api.exception.UserBindRequiredException
import xyz.xszq.bot.api.exception.UserDeniedException
import xyz.xszq.bot.api.exception.UserNotFoundException
import xyz.xszq.bot.api.exception.UserOARequiredException
import xyz.xszq.bot.database.QQBindTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.music.RatingResponse
import xyz.xszq.bot.music.Record
import xyz.xszq.bot.music.RecordsResponse
import xyz.xszq.bot.newLine
import xyz.xszq.bot.reply

typealias MaimaiErrorHandler = suspend MessageEvent.(Throwable, String) -> Boolean

class MaimaiQuery(
    val maimai: Maimai
) {
    /**
     * Messages.
     */
    val noBackendBindings = "您还未在查分器上绑定QQ号，请前往水鱼/落雪查分器设置您的QQ号。"
    val noQQBindings = "为了继续后续查询，请输入“/bind qq号”绑定您的QQ号："
    val noRecords = "在当前筛选条件下未查询到歌曲记录。"
    val userNotFound = "您查询的用户不存在。"
    val userDenied = "您查询的用户设置了查分器隐私或未同意查分器协议，请检查设置。"
    val userEULA = "请先前往查分器同意用户协议再进行查询。"

    suspend fun messageUserNeedBind(
        event: MessageEvent,
        args: String
    ) = event.run {
        if (args.isBlank()) { // 查询绑定的用户
            if (QQBindTable.hasBinding(sender.id)) {
                if (event.textMode())
                    reply(noBackendBindings)
                else
                    reply(MarkdownTemplates.Templates.SELECT_BACKENDS)
            } else {
                if (event.textMode())
                    reply(noQQBindings)
                else
                    reply(MarkdownTemplates.Templates.BIND_QQ)
            }
        } else { // 指定的用户不存在
            reply(userNotFound)
        }
    }

    suspend fun messageUserDenied(
        event: MessageEvent,
        args: String
    ) = event.run {
        if (args.isBlank()) { // 查询绑定的用户
            if (event.textMode())
                reply(userEULA)
            else
                reply(MarkdownTemplates.Templates.USER_EULA)
        } else { // 指定的用户不存在
            reply(userDenied)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun requestOA(
        event: MessageEvent
    ) = event.run {
        val token = UUID.randomUUID().toString()
        maimai.api.bindTokens[token] = this
        val authUrl = "https://bot-api.otmdb.cn/jump/lxns-oa/$token"

        if (textMode()) {
            reply(buildString {
                appendLine("使用该功能需要您授权BOT访问您的全部成绩信息。请您链接授权：")
                appendLine(authUrl)
            }.trim().newLine())
        } else {
            reply(MarkdownTemplates.Templates.oauth(authUrl))
        }
        GlobalScope.launch {
            delay(5 * 60000L)
            maimai.api.bindTokens.remove(token)
        }
    }

    suspend fun <R> rating(
        event: MessageEvent,
        args: String,
        handler: suspend MessageEvent.(RatingResponse, MaimaiAPI) -> R?
    ): R? = event.run query@ {
        // Temporary fix
        // TODO: Process exception in a more elegant way
        var lastException: Throwable? = null
        val (response, backend) = maimai.backendsWithPriority(event, args).firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRating(this, args)
            }.onFailure { e ->
                if (errorHandler(e, args))
                    return@query null
                lastException = e
            }.getOrNull() ?.let { Pair(it, backend) }
        } ?: run {
            if (lastException is UserNotFoundException)
                messageUserNeedBind(this, args)
            else
                reply("查询失败")
            return@query null
        }
        return handler(response, backend)
    }

    suspend fun <R> records(
        event: MessageEvent,
        musics: List<MusicInfo>,
        args: String = "",
        handler: suspend MessageEvent.(RecordsResponse, MaimaiAPI) -> R?
    ): R? = event.run query@ {
        var lastException: Throwable? = null
        val (response, backend) = maimai.backendsWithPriority(event, args).firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRecords(this, args, musics)
            }.onFailure { e ->
                if (errorHandler(e, args))
                    return@query null
                lastException = e
            }.getOrNull() ?.let { Pair(it, backend) }
        } ?: run {
            if (lastException is UserNotFoundException)
                messageUserNeedBind(this, args)
            else
                reply("查询失败")
            return@query null
        }
        return handler(response, backend)
    }

    suspend fun records(
        event: MessageEvent,
        musics: List<MusicInfo>
    ) = runCatching {
        records(event, musics) { result, _ -> result }
    }.getOrNull()

    suspend fun <R> record(
        event: MessageEvent,
        music: MusicInfo,
        handler: suspend MessageEvent.(List<Record>) -> R?
    ): R? = event.run query@ {
        var lastException: Throwable? = null
        val response = maimai.backendsWithPriority(event, "").firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRecord(this, "", music)
            }.onFailure { e ->
                if (errorHandler(e, ""))
                    return@query null
                lastException = e
            }.getOrNull()
        } ?: run {
            if (lastException is UserNotFoundException)
                messageUserNeedBind(this, "")
            else
                reply("查询失败")
            return@query null
        }
        return handler(response)
    }

    val errorHandler: MaimaiErrorHandler = handler@ { e, args ->
        when (e) {
            is UserNotFoundException -> {
                return@handler false
            }
            is UserBindRequiredException -> {
                messageUserNeedBind(this, args)
            }
            is UserDeniedException -> {
                messageUserDenied(this, args)
            }
            is UserOARequiredException -> {
                requestOA(this)
            }
            else -> {
                e.printStackTrace()
            }
        }
        return@handler true
    }
}