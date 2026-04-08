package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import okhttp3.ConnectionPool
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import java.util.concurrent.TimeUnit


/**
 * API connects to QQ Server.
 * @param config Bot's config.
 */
class OpenAPI(
    val config: BotConfig,
    var filter: WordFilter,
    private val client: HttpClient = defaultHttpClient(),
    private val server: String = DEFAULT_SERVER,
    private val accessTokenUrl: String = DEFAULT_ACCESS_TOKEN_URL,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val logger = KotlinLogging.logger {}

    private var accessToken: String? = null
    private var accessTokenExpiresAt: Long? = null

    companion object {
        const val DEFAULT_SERVER = "https://api.sgroup.qq.com"
        const val DEFAULT_ACCESS_TOKEN_URL = "https://bots.qq.com/app/getAppAccessToken"

        fun defaultHttpClient() = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(json)
            }
            engine {
                config {
                    connectionPool(ConnectionPool(50, 30, TimeUnit.SECONDS))
                }
            }
        }
    }

    private suspend fun getRawAccessToken() =
        client.post(accessTokenUrl) {
            contentType(ContentType.Application.Json)
            setBody(AccessTokenRequest(config.appId, config.clientSecret))
        }.body<AccessTokenResponse>()

    private suspend fun getToken(): String {
        val currentTime = now()
        if (accessToken.isNullOrEmpty() || accessTokenExpiresAt?.let { currentTime > it } == true) {
            getRawAccessToken().let { response ->
                accessToken = response.accessToken
                accessTokenExpiresAt = currentTime + response.expiresIn * 1000L
            }
        }
        return checkNotNull(accessToken)
    }

    private suspend fun HttpRequestBuilder.setToken() {
        headers["Authorization"] = "QQBot ${getToken()}"
        headers["X-Union-Appid"] = config.appId
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
