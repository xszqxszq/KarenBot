package xyz.xszq.bot.controller

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.xszq.bot.Maimai
import xyz.xszq.bot.api.LXNS
import xyz.xszq.bot.component.WaitingEventData
import xyz.xszq.bot.database.DivingFishBindTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.reply
import java.util.concurrent.ConcurrentHashMap

class ApiController(
    val maimai: Maimai
) {
    lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    val lxnsBindTokens = ConcurrentHashMap<String, WaitingEventData>()
    val updateTokens = ConcurrentHashMap<String, WaitingEventData>()
    var proxyServer: String = ""
    var proxyPort: Int = 0

    private val client = HttpClient {

    }
    private val redirectClient = HttpClient {
        followRedirects = false
    }

    init {
        proxyServer = maimai.config.tokens["proxy-server"] ?: throw NotFoundException()
        proxyPort = maimai.config.tokens["proxy-port"] ?.toInt() ?: throw NotFoundException()
        maimai.scope.launch {
            while (isActive) {
                delay(60_000L)
                val now = System.currentTimeMillis()
                lxnsBindTokens.entries.removeIf { it.value.expireAt < now }
                updateTokens.entries.removeIf { it.value.expireAt < now }
            }
        }
    }

    fun listen() = embeddedServer(Netty, host = "0.0.0.0", port = 18100) {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.httpMethod.value == "CONNECT") {
                call.respond(HttpStatusCode.OK)
                finish()
                return@intercept
            }
        }
        routing {
            get("/lxns/callback") {
                val query = call.request.queryParameters
                val code = query["code"] ?: return@get
                val state = query["state"] ?: return@get
                val event = lxnsBindTokens[state] ?.event ?: return@get
                lxnsBindTokens.remove(state)
                if ((maimai.backend("lxns") as LXNS).initOAuth(code, event)) {
                    event.reply("绑定成功。")
                    event.bot.pluginLoader.subscribes.handle(event)
                    call.respondText("绑定成功，您可以返回继续使用相关功能了。")
                } else {
                    event.reply("绑定失败，请重试。")
                }
            }
            get("/update") {
                val query = call.request.queryParameters
                val token = query["token"] ?: run {
                    call.respondText("缺少Token", status = HttpStatusCode.BadRequest)
                    return@get
                }

                val response = redirectClient.get(wahlapAuthorizeUrl) {
                    userAgent(userAgent)
                }
                val target = response.headers[HttpHeaders.Location] ?: run {
                    call.respondText("重定向错误", status = HttpStatusCode.BadGateway)
                    return@get
                }
                val callback = Url(target).parameters["redirect_uri"] ?: return@get
                val redirectUri = URLBuilder(callback).apply {
                    parameters["token"] = token
                    protocol = URLProtocol.HTTP
                }.buildString()

                val nowRedirect = URLBuilder(target).apply {
                    parameters["redirect_uri"] = redirectUri
                }.build()

                call.respondRedirect(nowRedirect)
            }
            get("/proxy-config/{type}") {
                val type = call.parameters["type"] ?: run {
                    call.respondText("未指定类型", status = HttpStatusCode.BadRequest)
                    return@get
                }
                when (type) {
                    "sing-box" -> call.respondText(contentType = ContentType.Application.Json) { singBox }
                    "clash" -> call.respondText(clash)
                }
            }
            host("tgk-wcaime.wahlap.com") {
                get("/wc_auth/oauth/callback/maimai-dx") {
                    val query = call.request.queryParameters
                    val token = query["token"] ?: run {
                        call.respondText("缺少Token", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    val data = updateTokens[token] ?: run {
                        call.respondText("Token不存在", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    maimai.scope.launch {
                        update(data.event, call.request.uri)
                    }
                    call.respondText("BOT正在更新中，您可以关闭此页面了。")
                    data.event.reply("正在爬取数据中……")
                }
            }
        }
    }.start(wait = false).also { server = it }

    fun close() {
        server.stop()
    }

    private val wahlapAuthorizeUrl = "https://tgk-wcaime.wahlap.com/wc_auth/oauth/authorize/maimai-dx"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/132.0.0.0 Safari/537.36 NetType/WIFI MicroMessenger/7.0.20.1781(0x6700143B) " +
            "WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf254162e) XWEB/18163 Flue"

    suspend fun update(
        event: MessageEvent,
        uri: String
    ) = event.run {
        val results = fetch(uri) ?: run {
            reply("更新失败，请稍后重试")
            return
        }
        val importToken = DivingFishBindTable[sender.id] ?: return
        results.forEach { recordHtml ->
            client.post(
                "https://www.diving-fish.com/api/maimaidxprober/player/update_records_html"
            ) {
                headers {
                    append("Import-Token", importToken)
                }
                setBody(recordHtml)
            }
        }
        reply("更新成功！")
    }

    suspend fun fetch(
        uri: String
    ): List<String>? {
        val getRedirectUrl = URLBuilder("https://tgk-wcaime.wahlap.com$uri").apply {
            parameters.remove("token")
        }.build()
        val getRedirectResponse = redirectClient.get(getRedirectUrl)

        val getCookieUrl = getRedirectResponse.headers[HttpHeaders.Location] ?: return null
        val getCookieResponse = redirectClient.get(getCookieUrl)
        val cookieString = getCookieResponse.headers.getAll(HttpHeaders.SetCookie)
            ?.joinToString("; ") { it.substringBefore(";") }
            ?: return null

        val htmlResults = mutableListOf<String>()

        (0..4).forEach { difficulty ->
            val recordResponse = client.get("https://maimai.wahlap.com/maimai-mobile/record/musicSort/search/") {
                parameter("search", "A")
                parameter("sort", "1")
                parameter("playCheck", "on")
                parameter("diff", difficulty)

                header(HttpHeaders.Cookie, cookieString)
                header(HttpHeaders.UserAgent, userAgent)
            }
            val content = recordResponse.bodyAsText()
            if ("错误码：" in content)
                return null
            htmlResults.add(content)
        }
        return htmlResults
    }

    val singBox
        get() = "{\"log\":{\"level\":\"info\",\"timestamp\":true},\"experimental\":{\"clash_api\":{\"external_controller\":\"127.0.0.1:9090\",\"external_ui\":\"ui\",\"default_mode\":\"rule\"}},\"inbounds\":[{\"type\":\"mixed\",\"tag\":\"mixed-in\",\"listen\":\"127.0.0.1\",\"listen_port\":2080,\"sniff\":true,\"sniff_override_destination\":true}],\"outbounds\":[{\"type\":\"http\",\"tag\":\"maimai-proxy\",\"server\":\"$proxyServer\",\"server_port\":$proxyPort},{\"type\":\"direct\",\"tag\":\"direct\"},{\"type\":\"dns\",\"tag\":\"dns-out\"}],\"route\":{\"rules\":[{\"domain_suffix\":[\"tgk-wcaime.wahlap.com\"],\"outbound\":\"maimai-proxy\"},{\"protocol\":\"dns\",\"outbound\":\"dns-out\"}],\"final\":\"direct\",\"auto_detect_interface\":true}}"
    val clash
        get() = "port: 7890\nsocks-port: 7891\nmode: rule\nproxies:\n  - name: 舞萌DX成绩更新代理\n    server: $proxyServer\n    port: $proxyPort\n    type: http\nproxy-groups:\n  - name: default\n    type: select\n    proxies:\n      - 舞萌DX成绩更新代理\nrules:\n  - DOMAIN-SUFFIX,tgk-wcaime.wahlap.com,default\n  - MATCH,DIRECT"
}