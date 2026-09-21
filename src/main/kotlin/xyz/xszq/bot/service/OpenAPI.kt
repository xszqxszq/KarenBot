package xyz.xszq.bot.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import okhttp3.ConnectionPool
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.exception.SendException
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.util.errorLogger
import xyz.xszq.bot.util.json
import xyz.xszq.bot.util.Metrics
import java.util.concurrent.TimeUnit


/**
 * 连接 QQ 服务器的客户端
 *
 * @property config 机器人配置
 */
class OpenAPI(
    var config: BotConfig,
    var filter: WordFilter,
    private val client: HttpClient = defaultHttpClient(),
    private val server: String = DEFAULT_SERVER,
    private val accessTokenUrl: String = DEFAULT_ACCESS_TOKEN_URL,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val logger = KotlinLogging.logger {}

    private var accessToken: String? = null
    private var accessTokenExpiresAt: Long? = null

    /**
     * 重载 Bot 配置
     *
     * @param config Bot 配置
     */
    fun reloadConfig(config: BotConfig) {
        this.config = config
        accessToken = null
        accessTokenExpiresAt = null
    }

    @Suppress("unused")
    companion object {
        const val DEFAULT_SERVER = "https://api.bot.qq.com"
        const val DEFAULT_ACCESS_TOKEN_URL = "https://api.bot.qq.com/app/getAppAccessToken"

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
        Metrics.time(
            "karenbot.qq.request",
            "api" to "token"
        ) {
            client.post(accessTokenUrl) {
                contentType(ContentType.Application.Json)
                setBody(AccessTokenRequest(config.appId, config.clientSecret))
            }.body<AccessTokenResponse>()
        }

    private suspend fun getToken(): String {
        val currentTime = now()
        if (accessToken.isNullOrEmpty() || accessTokenExpiresAt?.let { currentTime > it } == true) {
            getRawAccessToken().let { response ->
                accessToken = response.accessToken
                accessTokenExpiresAt = currentTime + response.expiresIn.toLong() * 1000L
            }
        }
        return checkNotNull(accessToken)
    }

    private suspend fun HttpRequestBuilder.setToken() {
        headers["Authorization"] = "QQBot ${getToken()}"
        headers["X-Union-Appid"] = config.appId
    }

    private suspend inline fun <reified T> HttpResponse.result(
        quiet: Boolean = false,
        log: () -> Unit = {}
    ): T? {
        if (status.isSuccess()) {
            log()
            return body<T>()
        }
        val error = body<ErrorResponse>()
        if (quiet)
            logger.debug { "[${error.code}] ${error.message}" }
        else
            errorLogger.error { "[${error.code}] ${error.message}" }
        return null
    }
    private suspend inline fun <reified T> HttpResponse.resultOrThrow(): T? {
        if (status.isSuccess()) {
            return body<T>()
        }
        val error = body<ErrorResponse>()
        throw SendException(error)
    }
    private suspend fun sendMessage(
        url: String,
        payload: MessagePayload,
        api: String
    ): Boolean {
        payload.content = filter.filter(payload.content)
        payload.markdown ?.let { markdown ->
            markdown.params ?.forEach { param ->
                param.values = param.values.map { filter.filter(it) }
            }
            markdown.content ?.let { content ->
                markdown.content = filter.filter(content)
            }
        }
        var outcome = "success"
        val result = Metrics.time(
            "karenbot.qq.request",
            "api" to api
        ) {
            kotlin.runCatching {
                client.post(url) {
                    contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    setBody(payload)
                    setToken()
                }.resultOrThrow<MessageResponse>()
            }.onFailure { e ->
                if (e is CancellationException)
                    throw e
                if (e !is SendException) {
                    outcome = "transport_error"
                    e.printStackTrace()
                } else {
                    outcome = when {
                        e.response.code == 429 -> "rate_limited"
                        e.response.code in 400..499 -> "client_error"
                        else -> "server_error"
                    }
                    when (e.response.code) {
                        40054005 -> {
                            payload.msgSeq += 1
                        }
                        else -> {}
                    }
                    errorLogger.error { "[${e.response.code}] ${e.response.message}" }
                }
            }.getOrNull()
        }
        Metrics.count(
            "karenbot.qq.request.total",
            "api" to api,
            "outcome" to outcome
        )
        return result != null
    }

    /**
     * 发送私聊消息
     *
     * @param user 目标用户 ID
     * @param content 文本内容
     * @param msgType 消息类型
     * @param markdown Markdown 消息
     * @param keyboard 键盘按钮
     * @param eventId 事件 ID
     * @param msgId 被回复的消息 ID
     * @param msgSeq 消息序号，用于同一条消息的幂等
     * @param media 已上传的媒体文件
     * @return 是否发送成功
     */
    suspend fun sendC2CMessage(
        user: String,
        content: String,
        msgType: Int,
        markdown: MarkdownData ?= null,
        keyboard: Keyboard ?= null,
        eventId: String ?= null,
        msgId: String ?= null,
        msgSeq: Int = 1,
        media: FileResponse? = null,
    ) = sendMessage(
        url = "$server/v2/users/$user/messages",
        payload = MessagePayload(
            content = content,
            msgType = msgType,
            markdown = markdown,
            keyboard = keyboard,
            eventId = eventId,
            msgId = msgId,
            msgSeq = msgSeq,
            media = media
        ),
        api = "send-c2c"
    )

    /**
     * 发送群聊消息
     *
     * @param group 目标群 ID
     * @param content 文本内容
     * @param msgType 消息类型
     * @param markdown Markdown 消息
     * @param keyboard 键盘按钮
     * @param eventId 事件 ID
     * @param msgId 被回复的消息 ID
     * @param msgSeq 消息序号，用于同一条消息的幂等
     * @param media 已上传的媒体文件
     * @return 是否发送成功
     */
    suspend fun sendGroupMessage(
        group: String,
        content: String,
        msgType: Int,
        markdown: MarkdownData ?= null,
        keyboard: Keyboard ?= null,
        eventId: String ?= null,
        msgId: String ?= null,
        msgSeq: Int = 1,
        media: FileResponse? = null,
    ) = sendMessage(
        url = "$server/v2/groups/$group/messages",
        payload = MessagePayload(
            content = content,
            msgType = msgType,
            markdown = markdown,
            keyboard = keyboard,
            eventId = eventId,
            msgId = msgId,
            msgSeq = msgSeq,
            media = media
        ),
        api = "send-group"
    )

    /**
     * 上传文件到私聊对话
     *
     * @param user 目标用户 ID
     * @param fileType 文件类型
     * @param url 文件 URL
     * @param srvSendMsg 是否直接发出
     */
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

    /**
     * 上传文件到群聊
     *
     * @param group 目标群 ID
     * @param fileType 文件类型
     * @param url 文件 URL
     * @param srvSendMsg 是否直接发出
     */
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

    /**
     * 撤回私聊消息
     *
     * @param user 目标用户 ID
     * @param messageId 要撤回的消息 ID
     * @return 是否撤回成功
     */
    suspend fun recallC2CMessage(
        user: String,
        messageId: String
    ) = client.delete("$server/v2/users/$user/messages/$messageId") {
        setToken()
    }.status == HttpStatusCode.OK

    /**
     * 撤回群聊消息
     *
     * @param group 目标群 ID
     * @param messageId 要撤回的消息 ID
     * @return 是否撤回成功
     */
    suspend fun recallGroupMessage(
        group: String,
        messageId: String
    ) = client.delete("$server/v2/groups/$group/messages/$messageId") {
        setToken()
    }.status == HttpStatusCode.OK

    /**
     * 获取 Bot 自身详情
     */
    suspend fun getMe(): UsersMeResponse? = client.get("$server/users/@me") {
        setToken()
    }.result<UsersMeResponse>()

    /**
     * 获取群基本信息
     *
     * @param group 目标群 ID
     */
    suspend fun getGroupInfo(group: String): GroupInfoResponse? =
        client.get("$server/v2/groups/$group/info") {
            setToken()
        }.result<GroupInfoResponse>()

    /**
     * 获取机器人群内状态
     *
     * @param group 目标群 ID
     */
    suspend fun getBotState(group: String): BotStateResponse? =
        client.get("$server/v2/groups/$group/bot_state") {
            setToken()
        }.result<BotStateResponse>(quiet = true)

    /**
     * 获取入群申请列表
     *
     * @param group 目标群 ID
     * @param cursor 分页游标
     * @param limit 单页数量
     */
    suspend fun getJoinRequestList(
        group: String,
        cursor: String = "",
        limit: Int = 20
    ): JoinRequestListResponse? =
        client.get("$server/v2/groups/$group/join_request_list") {
            setToken()
            parameter("cursor", cursor)
            parameter("limit", limit)
        }.result<JoinRequestListResponse>(quiet = true)

    /**
     * 查询群禁言状态
     *
     * @param group 目标群 ID
     */
    suspend fun getRestrictChatSetting(
        group: String
    ): RestrictChatSettingResponse? =
        client.get("$server/v2/groups/$group/restrict_chat_setting") {
            setToken()
        }.result<RestrictChatSettingResponse>(quiet = true)
}