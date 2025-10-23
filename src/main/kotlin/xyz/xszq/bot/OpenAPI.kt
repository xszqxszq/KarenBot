package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.payload.AccessTokenRequest
import xyz.xszq.bot.payload.AccessTokenResponse
import xyz.xszq.bot.payload.ErrorResponse
import xyz.xszq.bot.payload.FilePayload
import xyz.xszq.bot.payload.FileResponse
import xyz.xszq.bot.payload.MessagePayload
import xyz.xszq.bot.payload.MessageResponse
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData


/**
 * API connects to QQ Server.
 * @param config Bot's config.
 */
class OpenAPI(
    val config: BotConfig,
    val filter: WordFilter
) {
    val logger = KotlinLogging.logger {}

    private val server = "https://api.sgroup.qq.com"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    private var accessToken: String? = null
    private var accessTokenExpiresAt: Long? = null

    private suspend fun getRawAccessToken() =
        client.post("https://bots.qq.com/app/getAppAccessToken") {
            contentType(ContentType.Application.Json)
            setBody(AccessTokenRequest(config.appId, config.clientSecret))
        }.body<AccessTokenResponse>()

    private suspend fun getToken(): String {
        val now = System.currentTimeMillis()
        if (accessToken.isNullOrEmpty() || accessTokenExpiresAt?.let { now > it } == true) {
            getRawAccessToken().let { response ->
                accessToken = response.accessToken
                accessTokenExpiresAt = now + response.expiresIn * 1000L
            }
        }
        return checkNotNull(accessToken)
    }

    private fun HttpRequestBuilder.setToken() {
        runBlocking {
            headers["Authorization"] = "QQBot ${getToken()}"
            headers["X-Union-Appid"] = config.appId
        }
    }

    private suspend inline fun <reified T> HttpResponse.result(log: () -> Unit): T? {
        if (status.isSuccess()) {
            log()
            return body<T>()
        }
        val error = body<ErrorResponse>()
        logger.error { "[${error.code}] ${error.message}" }
        return null
    }
    private suspend inline fun <reified T> HttpResponse.result(): T? = result {  }
    private suspend inline fun <reified T> HttpResponse.resultOrThrow(): T? {
        if (status.isSuccess()) {
            return body<T>()
        }
        val error = body<ErrorResponse>()
        throw SendException(error)
    }
    private suspend fun sendMessage(
        url: String,
        payload: MessagePayload
    ): Boolean {
        payload.content = filter.filter(payload.content)
        payload.markdown ?.let { markdown ->
            markdown.params.forEach { param ->
                param.values = param.values.map { filter.filter(it) }
            }
        }
        val result = kotlin.runCatching {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload)
                setToken()
            }.resultOrThrow<MessageResponse>()
        }.onFailure { e ->
            if (e !is SendException) {
                e.printStackTrace()
                return false
            }
            when (e.response.code) {
                40054005 -> {
                    payload.msgSeq += 1
                }
                else -> {}
            }
            logger.error { "[${e.response.code}] ${e.response.message}" }
        }.getOrNull()
        return result != null
    }
    suspend fun sendC2CMessage(
        user: String,
        content: String,
        msgType: Int,
        markdown: MarkdownData ?= null,
        keyboard: Keyboard ?= null,
        eventId: String,
        msgId: String,
        msgSeq: Int = 1,
        media: FileResponse? = null,
    ) = sendMessage("$server/v2/users/$user/messages", MessagePayload(
        content = content,
        msgType = msgType,
        markdown = markdown,
        keyboard = keyboard,
        eventId = eventId,
        msgId = msgId,
        msgSeq = msgSeq,
        media = media
    ))

    suspend fun sendGroupMessage(
        group: String,
        content: String,
        msgType: Int,
        markdown: MarkdownData ?= null,
        keyboard: Keyboard ?= null,
        eventId: String,
        msgId: String,
        msgSeq: Int = 1,
        media: FileResponse? = null,
    ) = sendMessage("$server/v2/groups/$group/messages", MessagePayload(
        content = content,
        msgType = msgType,
        markdown = markdown,
        keyboard = keyboard,
        eventId = eventId,
        msgId = msgId,
        msgSeq = msgSeq,
        media = media
    ))

    suspend fun uploadC2CFile(
        user: String,
        fileType: Int,
        url: String,
        srvSendMsg: Boolean = false
    ) = client.post("$server/v2/users/$user/files") {
        contentType(ContentType.Application.Json)
        setBody(FilePayload(
            fileType = fileType,
            url = url,
            srvSendMsg = srvSendMsg
        ))
        setToken()
    }.result<FileResponse>()

    suspend fun uploadGroupFile(
        group: String,
        fileType: Int,
        url: String,
        srvSendMsg: Boolean = false
    ) = client.post("$server/v2/groups/$group/files") {
        contentType(ContentType.Application.Json)
        setBody(FilePayload(
            fileType = fileType,
            url = url,
            srvSendMsg = srvSendMsg
        ))
        setToken()
    }.result<FileResponse>()
}