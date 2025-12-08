package xyz.xszq.bot.controller

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.api.LXNS
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.reply
import java.util.concurrent.ConcurrentHashMap

class ApiController(
    val maimai: Maimai
) {
    lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    val bindTokens = ConcurrentHashMap<String, MessageEvent>()
    fun listen() = embeddedServer(Netty, host = "0.0.0.0", port = 18100) {
        routing {
            get("/lxns/callback") {
                val query = call.request.queryParameters
                val code = query["code"] ?: return@get
                val state = query["state"] ?: return@get
                val event = bindTokens[state] ?: return@get
                bindTokens.remove(state)
                if ((maimai.backend("lxns") as LXNS).initOAuth(code, event)) {
                    event.reply("绑定成功。")
                    event.bot.pluginLoader.subscribes.handle(event)
                    call.respondText("绑定成功，您可以返回继续使用相关功能了。")
                } else {
                    event.reply("绑定失败，请重试。")
                }
            }
        }
    }.start(wait = false).also { server = it }
    fun close() {
        server.stop()
    }
}