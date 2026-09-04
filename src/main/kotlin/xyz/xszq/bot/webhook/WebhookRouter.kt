package xyz.xszq.bot.webhook

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import xyz.xszq.bot.*
import xyz.xszq.bot.config.ForwardConfig
import xyz.xszq.bot.payload.OpCode
import xyz.xszq.bot.payload.Payload
import xyz.xszq.bot.payload.WebhookValidation
import xyz.xszq.bot.util.handleValidation
import xyz.xszq.bot.util.verifyBody
import xyz.xszq.bot.service.WordFilter

/**
 * Webhook 路由处理
 *
 * @property logger 日志器
 * @property pluginLoader 插件加载器
 * @property filter 敏感词过滤器
 * @property forwardConfig 转发配置提供者
 */
class WebhookRouter(
    private val logger: KLogger,
    private val pluginLoader: PluginLoader,
    private val filter: WordFilter,
    private val forwardConfig: () -> ForwardConfig?
) {
    private val forwarder = WebhookForwarder(forwardConfig)
    private val dispatcher = WebhookDispatcher(forwarder)

    /**
     * 注册 Webhook 路由
     */
    fun configure(application: Application) = application.routing {
        get("/") {
            call.respondText { "200 OK" }
        }
        post("/webhook") {
            // 校验请求头
            if (call.request.headers["User-Agent"] != "QQBot-Callback"
                || call.request.headers["X-Bot-Appid"] != pluginLoader.api.config.appId)
                return@post call.respond(HttpStatusCode.BadRequest)
            val signature = call.request.headers["X-Signature-Ed25519"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val timestamp = call.request.headers["X-Signature-Timestamp"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)

            // 校验请求体签名
            val body = call.receiveText()
            if (!verifyBody(pluginLoader.api.config.clientSecret, signature, timestamp, body))
                return@post call.respond(HttpStatusCode.Unauthorized)

            // 处理负荷
            handleWebhook(application, call, body)
        }
    }

    /**
     * 解析负荷并处理
     *
     * @param application 应用实例，用于事件分发
     * @param call 收到的请求
     * @param body 原始请求体
     */
    private suspend fun handleWebhook(
        application: Application,
        call: RoutingCall,
        body: String
    ) = kotlin.runCatching {
        val payload = json.decodeFromString<Payload>(body)
        logger.debug { body }
        when (payload.op) {
            // 收到事件
            OpCode.DISPATCH -> with(dispatcher) {
                // 确认收到
                call.respond(Payload(OpCode.HTTP_CALLBACK_ACK))
                // 分发事件
                application.dispatch(
                    payload = payload,
                    call = call,
                    logger = logger,
                    filter = filter,
                    pluginLoader = pluginLoader,
                    body = body
                )
            }
            // 收到 Webhook 地址验证
            OpCode.HTTP_CALLBACK_VALIDATE -> {
                handleValidation(
                    pluginLoader.api.config.clientSecret,
                    json.decodeFromString<WebhookValidation>(payload.d!!)
                ) ?.let { response ->
                    call.respond(response)
                } ?: run {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
            // 未知操作码
            else -> {
                call.respond(HttpStatusCode.NotAcceptable)
            }
        }
    }.onFailure {
        // 未处理的异常
        it.printStackTrace()
        call.respond(HttpStatusCode.InternalServerError)
    }
}