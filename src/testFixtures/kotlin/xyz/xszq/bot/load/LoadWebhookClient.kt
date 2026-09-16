package xyz.xszq.bot.load

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xyz.xszq.bot.util.buildSeed
import xyz.xszq.bot.util.signMessage
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.text.Charsets.UTF_8

/**
 * Webhook 压测客户端
 *
 * @property port Webhook 服务端口
 * @property appId 机器人 AppID
 * @property clientSecret 机器人客户端密钥
 */
class LoadWebhookClient(
    private val port: Int,
    private val appId: String,
    private val clientSecret: String
) {
    private val seed = buildSeed(clientSecret)
    private val client: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /**
     * 构造群聊消息事件
     *
     * @param eventId 事件 ID
     * @param group 群 OpenID
     * @param user 发送者 OpenID
     * @param content 消息内容
     * @return 原始请求
     */
    fun groupMessage(
        eventId: String,
        group: String,
        user: String,
        content: String
    ): String {
        val data = buildJsonObject {
            put("id", "msg-$eventId")
            put("author", buildJsonObject {
                put("member_openid", user)
                put("username", "load-$user")
                put("bot", false)
                put("scope", "single")
                put("is_you", false)
                put("member_role", "member")
            })
            put("content", content)
            put("timestamp", "0")
            put("group_openid", group)
        }
        return buildJsonObject {
            put("op", 0)
            put("id", eventId)
            put("t", "GROUP_MESSAGE_CREATE")
            put("d", data)
        }.toString()
    }

    /**
     * 提交一个事件
     *
     * @param body 原始请求
     * @return 从发出到收到 ACK 的耗时（纳秒）
     */
    suspend fun post(body: String): Long {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature = signMessage(seed, (timestamp + body).toByteArray(UTF_8))
        val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/webhook"))
            .header("User-Agent", "QQBot-Callback")
            .header("X-Bot-Appid", appId)
            .header("X-Signature-Ed25519", signature)
            .header("X-Signature-Timestamp", timestamp)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, UTF_8))
            .build()
        val start = System.nanoTime()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        val elapsed = System.nanoTime() - start
        check(response.statusCode() == 200) {
            "Webhook 返回 HTTP ${response.statusCode()}"
        }
        return elapsed
    }

    /**
     * 等待 Webhook 服务就绪
     *
     * @param timeoutMs 等待上限（毫秒）
     */
    suspend fun awaitReady(timeoutMs: Long = 15_000) {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000L
        while (System.nanoTime() < deadline) {
            val ready = runCatching {
                val request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build()
                client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200
            }.getOrDefault(false)
            if (ready)
                return
            delay(50)
        }
        error("Webhook 服务未在 ${timeoutMs}ms 内就绪")
    }

    /**
     * 关闭底层连接
     */
    fun close() {
        client.close()
    }

    private suspend fun <T> CompletableFuture<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            whenComplete { value, error ->
                if (error != null)
                    continuation.resumeWithException(error)
                else
                    continuation.resume(value)
            }
            continuation.invokeOnCancellation { cancel(true) }
        }
}
