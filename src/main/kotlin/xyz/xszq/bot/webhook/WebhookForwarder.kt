package xyz.xszq.bot.webhook

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.server.routing.RoutingCall
import xyz.xszq.bot.util.AllTrustManager
import xyz.xszq.bot.config.ForwardConfig
import java.security.SecureRandom
import javax.net.ssl.SSLContext

/**
 * Webhook 请求转发
 *
 * @property config 转发配置
 */
class WebhookForwarder(
    private val config: () -> ForwardConfig?
) {
    private val trustAllManager = AllTrustManager()
    private val trustAllSslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(trustAllManager), SecureRandom())
    }
    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    sslSocketFactory(trustAllSslContext.socketFactory, trustAllManager)
                    hostnameVerifier { _, _ -> true }
                }
            }
        }
    }
    private val forwardHeaders = listOf(
        "User-Agent",
        "X-Bot-Appid",
        "X-Signature-Ed25519",
        "X-Signature-Timestamp"
    )

    /**
     * 判断事件应当转发给哪一目标
     *
     * @param subject 私聊 ID，仅私聊类事件传入
     * @param group 群 ID，仅群聊类事件传入
     * @return 转发目标地址，未命中时返回 null
     */
    fun forwardTarget(
        subject: String ?= null,
        group: String ?= null
    ): String? = config()?.let { config ->
        when {
            group != null && group in config.groups -> config.whitelist
            subject != null && subject in config.subjects -> config.whitelist
            config.otherwise.isNotBlank() -> config.otherwise
            else -> null
        }
    }

    /**
     * 原样转发请求到目标处
     *
     * @param body 原始请求体
     * @param call 收到的请求
     * @param to 目标地址
     */
    suspend fun forward(
        body: String,
        call: RoutingCall,
        to: String
    ) {
        val requested = call.request.headers
        client.post(to) {
            forwardHeaders.forEach { header ->
                header(header, requested[header] ?: "")
            }
            setBody(body)
        }
    }
}