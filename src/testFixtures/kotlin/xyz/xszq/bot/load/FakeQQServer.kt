package xyz.xszq.bot.load

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import xyz.xszq.bot.config.BotConfig
import xyz.xszq.bot.config.DatabaseConfig
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.WordFilter
import xyz.xszq.bot.util.json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * 模拟 QQ 服务器
 *
 * @property appId 机器人 AppID
 * @property clientSecret 机器人客户端密钥
 * @property latencyMs 接口延迟（毫秒）
 */
class FakeQQServer(
    val appId: String = "test-app-id",
    val clientSecret: String = "test-client-secret",
    private val latencyMs: Long = 0
) {
    /**
     * 机器人发出的一条回复
     *
     * @property eventId 触发回复的事件 ID
     * @property target 接收方的群或用户 ID
     * @property text 回复文本
     * @property group 是否为群聊消息
     * @property atNanos 发出时刻
     */
    class QQReply(
        val eventId: String,
        val target: String,
        val text: String,
        val group: Boolean,
        val atNanos: Long
    )

    private val sequence = AtomicLong(0)
    private val waiters = ConcurrentHashMap<String, CompletableDeferred<QQReply>>()

    /**
     * 每个事件收到的第一条回复
     */
    val replies = ConcurrentHashMap<String, QQReply>()

    /**
     * 机器人发出的全部消息，同一事件可能有多条
     */
    val sends = ConcurrentLinkedQueue<QQReply>()

    /**
     * 连接 QQ 服务器的客户端
     */
    val api: OpenAPI = OpenAPI(
        config = BotConfig(
            appId = appId,
            clientSecret = clientSecret,
            port = 0,
            database = DatabaseConfig("jdbc:h2:mem:fake-qq", "org.h2.Driver", "", "")
        ),
        filter = WordFilter(emptyList()),
        client = client(),
        server = "http://127.0.0.1:1",
        accessTokenUrl = "http://127.0.0.1:1/getAppAccessToken"
    )

    /**
     * 等待指定事件的第一条回复
     *
     * @param eventId 事件 ID
     * @param timeoutMs 等待上限（毫秒）
     * @return 收到的回复
     */
    suspend fun awaitReply(eventId: String, timeoutMs: Long = 30_000): QQReply? =
        withTimeoutOrNull(timeoutMs) {
            waiters.computeIfAbsent(eventId) { CompletableDeferred() }.await()
        }

    /**
     * 清空已记录的回复
     */
    fun reset() {
        replies.clear()
        sends.clear()
        waiters.clear()
    }

    private fun client() = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith("/getAppAccessToken") -> jsonResponse(
                json.encodeToString(AccessTokenResponse("test-access-token", 7200))
            )
            path.endsWith("/users/@me") -> jsonResponse(
                json.encodeToString(UsersMeResponse(id = "test-bot", username = "测试机器人"))
            )
            request.method == HttpMethod.Delete -> respond("", HttpStatusCode.OK)
            path.endsWith("/files") -> {
                delay(latencyMs)
                jsonResponse(json.encodeToString(FileResponse("file-uuid", "file-info", 0)))
            }
            path.endsWith("/messages") -> send(request)
            else -> respond("not found", HttpStatusCode.NotFound)
        }
    }) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private suspend fun MockRequestHandleScope.send(request: HttpRequestData): HttpResponseData {
        val payload = runCatching {
            json.decodeFromString<MessagePayload>(request.bodyText())
        }.getOrNull()
        delay(latencyMs)
        val path = request.url.encodedPath
        val eventId = payload?.eventId ?: ""
        val reply = QQReply(
            eventId = eventId,
            target = path.removeSuffix("/messages")
                .removePrefix("/v2/groups/")
                .removePrefix("/v2/users/"),
            text = payload?.markdown?.content ?: payload?.content ?: "",
            group = "/groups/" in path,
            atNanos = System.nanoTime()
        )
        if (eventId.isNotBlank()) {
            replies[eventId] = reply
            waiters.computeIfAbsent(eventId) { CompletableDeferred() }.complete(reply)
        }
        sends += reply
        return jsonResponse(
            json.encodeToString(MessageResponse("msg-${sequence.incrementAndGet()}", "0"))
        )
    }

    private fun HttpRequestData.bodyText(): String =
        (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString() ?: ""

    private fun MockRequestHandleScope.jsonResponse(content: String) = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}
