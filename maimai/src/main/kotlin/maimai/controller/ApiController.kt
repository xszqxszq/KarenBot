package xyz.xszq.bot.maimai.controller

import io.ktor.client.*
import io.ktor.client.request.*
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
import xyz.xszq.bot.Group
import xyz.xszq.bot.Member
import xyz.xszq.bot.User
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.exception.NotFoundException
import xyz.xszq.bot.maimai.Maimai
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.component.WaitingEventData
import xyz.xszq.bot.maimai.database.DivingFishBindTable
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.ProberBindTable
import xyz.xszq.bot.maimai.database.RhythmGameTokens
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.reply
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ApiController(
    val maimai: Maimai
) {
    lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    val oauthBindTokens = ConcurrentHashMap<String, WaitingEventData>()
    val updateTokens = ConcurrentHashMap<String, WaitingEventData>()
    var proxyIP: String = ""
    var proxyServer: String = ""
    var proxyPort: Int = 0

    private val redirectClient = HttpClient {
        followRedirects = false
    }

    init {
        proxyIP = maimai.config.tokens["proxy-ip"] ?: throw NotFoundException()
        proxyServer = maimai.config.tokens["proxy-server"] ?: throw NotFoundException()
        proxyPort = maimai.config.tokens["proxy-port"] ?.toInt() ?: throw NotFoundException()
    }

    fun start() {
        maimai.scope.launch {
            restoreOAuthBindTokens()
            while (isActive) {
                delay(60_000L)
                val now = System.currentTimeMillis()
                oauthBindTokens.entries.removeIf { it.value.expireAt < now }
                updateTokens.entries.removeIf { it.value.expireAt < now }
                RhythmGameTokens.load().forEach { token ->
                    if (token.expiresAt < now)
                        RhythmGameTokens.remove(token.id)
                }
            }
        }
    }

    private suspend fun restoreOAuthBindTokens() {
        val now = System.currentTimeMillis()
        RhythmGameTokens.load().forEach { token ->
            if (token.expiresAt < now) {
                RhythmGameTokens.remove(token.id)
                return@forEach
            }
            val bot = maimai.pluginLoader.bot
            val message = MessageChain(token.message)
            val event = when (token.eventType) {
                "group" -> GroupMessageEvent(
                    bot = bot,
                    eventId = token.eventId,
                    id = token.messageId,
                    message = message,
                    sender = Member(bot, token.senderId),
                    group = Group(bot, token.groupId ?: return@forEach),
                    seq = token.seq
                )
                else -> MessageEvent(
                    bot = bot,
                    eventId = token.eventId,
                    id = token.messageId,
                    message = message,
                    sender = User(bot, token.senderId),
                    seq = token.seq
                )
            }
            oauthBindTokens[token.id] = WaitingEventData(event, token.replay, token.expiresAt)
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
            get("/diving-fish/device-bind/{token}") {
                val token = call.parameters["token"] ?: run {
                    call.respondText("Token不存在", status = HttpStatusCode.BadRequest)
                    return@get
                }
                val data = oauthBindTokens[token] ?: run {
                    call.respondText("绑定已过期，请重新发起绑定。", status = HttpStatusCode.BadRequest)
                    return@get
                }
                RhythmGameTokens.remove(token)
                val divingFish = maimai.backend("diving-fish") as DivingFish
                val openid = data.event.sender.id
                val auth = runCatching {
                    divingFish.deviceAuthorization(openid, openid)
                }.getOrElse {
                    call.respondText("发起绑定失败，请重试。", status = HttpStatusCode.BadGateway)
                    return@get
                }
                maimai.scope.launch {
                    val ok = divingFish.awaitDeviceBinding(
                        auth.deviceCode,
                        auth.interval,
                        auth.expiresIn
                    )
                    if (ok) {
                        runCatching {
                            divingFish.bindByRef(openid)
                        }
                        if (MaimaiSettingsTable[openid, "prober"]?.isNotBlank() == true)
                            MaimaiSettingsTable[openid, "prober"] = "diving-fish"
                        data.event.reply("绑定成功，您可正常使用舞萌/中二各功能了。")
                        if (data.replay)
                            maimai.pluginLoader.subscribes.handle(data.event)
                    }
                }
                call.respondRedirect(auth.verificationUriComplete)
            }
            get("/lxns/callback") {
                val query = call.request.queryParameters
                val code = query["code"] ?: return@get
                val state = query["state"] ?: return@get
                val data = oauthBindTokens[state] ?: return@get
                oauthBindTokens.remove(state)
                RhythmGameTokens.remove(state)
                if (query["error"] != null || code.isBlank()) {
                    data.event.reply("绑定失败，请重试。")
                    call.respondText("绑定失败，请重试。")
                    return@get
                }
                if ((maimai.backend("lxns") as LXNS).initOAuth(code, data.event)) {
                    if (MaimaiSettingsTable[data.event.sender.id, "prober"]?.isNotBlank() == true)
                        MaimaiSettingsTable[data.event.sender.id, "prober"] = "lxns"
                    data.event.reply("绑定成功，您可正常使用舞萌/中二各功能了。")
                    if (data.replay)
                        maimai.pluginLoader.subscribes.handle(data.event)
                    call.respondText("绑定成功，您可以返回继续使用相关功能了。")
                } else {
                    data.event.reply("绑定失败，请重试。")
                    call.respondText("绑定失败，请重试。")
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
                    "throne", "nekoray" -> call.respondText(contentType = ContentType.Application.Json) { throne }
                    "nekobox" -> call.respondText(contentType = ContentType.Application.Json) { nekoBox }
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
                        val importToken = DivingFishBindTable[data.event.sender.id] ?: return@launch
                        val divingFish = maimai.backend("diving-fish") as DivingFish
                        runCatching {
                            divingFish.update(call.request.uri, importToken)
                        }.onSuccess { result ->
                            data.event.reply("更新成功，已更新${result.creates}条记录。")
                        }.onFailure {
                            data.event.reply("更新失败，请稍后重试")
                        }
                    }
                    call.respondText("BOT正在更新中，您可以关闭此页面了。")
                    data.event.reply("正在爬取数据中……")
                }
            }
            get("/_cron/refresh-lxns") {
                val addr = call.request.local.remoteAddress
                if (addr != "127.0.0.1" && addr != "0:0:0:0:0:0:0:1") {
                    call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                    return@get
                }
                val lxns = maimai.backend("lxns") as LXNS
                val ids = ProberBindTable.idsForKey("lxns", "refresh")
                var ok = 0
                var fail = 0
                for (id in ids) {
                    if (runCatching { lxns.refreshToken(id) }.getOrDefault(false))
                        ok++
                    else
                        fail++
                    delay(Random.nextLong(1000, 30000))
                }
                call.respondText("ok=$ok fail=$fail")
            }
            get("/_cron/migrate-diving-fish") {
                val addr = call.request.local.remoteAddress
                if (addr != "127.0.0.1" && addr != "0:0:0:0:0:0:0:1") {
                    call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                    return@get
                }
                val limit = call.request.queryParameters["limit"] ?.toIntOrNull() ?: 2000
                val divingFish = maimai.backend("diving-fish") as DivingFish
                val (ok, fail) = divingFish.migrateQQBindings(limit)
                call.respondText("ok=$ok fail=$fail")
            }
            get("/_cron/migrate-lxns") {
                val addr = call.request.local.remoteAddress
                if (addr != "127.0.0.1" && addr != "0:0:0:0:0:0:0:1") {
                    call.respondText("forbidden", status = HttpStatusCode.Forbidden)
                    return@get
                }
                val lxns = maimai.backend("lxns") as LXNS
                val (ok, skip) = lxns.migrateLegacyBindings()
                call.respondText("ok=$ok skip=$skip")
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

    val singBox
        get() = "{\"log\":{\"level\":\"info\",\"timestamp\":true},\"inbounds\":[{\"type\":\"mixed\",\"tag\":\"mixed-in\",\"listen\":\"127.0.0.1\",\"listen_port\":2080,\"sniff\":true,\"sniff_override_destination\":true}],\"outbounds\":[{\"type\":\"http\",\"tag\":\"maimai-proxy\",\"server\":\"$proxyServer\",\"server_port\":$proxyPort},{\"type\":\"direct\",\"tag\":\"direct\"},{\"type\":\"dns\",\"tag\":\"dns-out\"}],\"route\":{\"rules\":[{\"domain_suffix\":[\"tgk-wcaime.wahlap.com\"],\"outbound\":\"maimai-proxy\"},{\"protocol\":\"dns\",\"outbound\":\"dns-out\"}],\"final\":\"direct\",\"auto_detect_interface\":true}}"
    val throne
        get() = "{\"log\":{\"level\":\"info\",\"timestamp\":true},\"experimental\":{\"clash_api\":{\"external_controller\":\"127.0.0.1:9090\",\"external_ui\":\"ui\",\"default_mode\":\"rule\",\"store_selected\":false}},\"inbounds\":[{\"type\":\"mixed\",\"tag\":\"mixed-in\",\"listen\":\"127.0.0.1\",\"listen_port\":2080,\"sniff\":true,\"sniff_override_destination\":true}],\"outbounds\":[{\"type\":\"http\",\"tag\":\"maimai-proxy\",\"server\":\"$proxyServer\",\"server_port\":$proxyPort},{\"type\":\"direct\",\"tag\":\"direct\"},{\"type\":\"dns\",\"tag\":\"dns-out\"}],\"route\":{\"rules\":[{\"domain_suffix\":[\"tgk-wcaime.wahlap.com\"],\"outbound\":\"maimai-proxy\"},{\"protocol\":\"dns\",\"outbound\":\"dns-out\"}],\"final\":\"direct\",\"auto_detect_interface\":true}}"
    val nekoBox
        get() = "{\"dns\":{\"final\":\"dns-remote\",\"independent_cache\":true,\"rules\":[{\"domain\":[\"$proxyServer\",\"tgk-wcaime.wahlap.com\",\"dns.google\"],\"server\":\"dns-direct\"},{\"outbound\":[\"any\"],\"server\":\"dns-direct\"}],\"servers\":[{\"address\":\"rcode://success\",\"tag\":\"dns-block\"},{\"address\":\"local\",\"detour\":\"direct\",\"tag\":\"dns-local\"},{\"address\":\"https://223.5.5.5/dns-query\",\"address_resolver\":\"dns-local\",\"detour\":\"direct\",\"strategy\":\"prefer_ipv4\",\"tag\":\"dns-direct\"},{\"address\":\"https://dns.google/dns-query\",\"address_resolver\":\"dns-direct\",\"strategy\":\"prefer_ipv4\",\"tag\":\"dns-remote\"}]},\"inbounds\":[{\"domain_strategy\":\"\",\"endpoint_independent_nat\":true,\"inet4_address\":[\"172.19.0.1/28\"],\"inet6_address\":[\"fdfe:dcba:9876::1/126\"],\"mtu\":9000,\"sniff\":true,\"sniff_override_destination\":true,\"stack\":\"mixed\",\"tag\":\"tun-in\",\"type\":\"tun\"},{\"domain_strategy\":\"\",\"listen\":\"127.0.0.1\",\"listen_port\":2080,\"sniff\":true,\"sniff_override_destination\":true,\"tag\":\"mixed-in\",\"type\":\"mixed\"}],\"log\":{\"level\":\"info\"},\"outbounds\":[{\"domain_strategy\":\"prefer_ipv4\",\"password\":\"\",\"server\":\"$proxyServer\",\"server_port\":$proxyPort,\"username\":\"\",\"tag\":\"proxy\",\"type\":\"http\"},{\"tag\":\"direct\",\"type\":\"direct\"}],\"route\":{\"auto_detect_interface\":true,\"rule_set\":[],\"rules\":[{\"action\":\"hijack-dns\",\"port\":[53]},{\"action\":\"hijack-dns\",\"protocol\":[\"dns\"]},{\"action\":\"route\",\"domain\":[\"tgk-wcaime.wahlap.com\"],\"outbound\":\"proxy\"},{\"action\":\"reject\",\"ip_cidr\":[\"224.0.0.0/3\",\"ff00::/8\"],\"source_ip_cidr\":[\"224.0.0.0/3\",\"ff00::/8\"]}]}}"
    val clash
        get() = "port: 7890\nsocks-port: 7891\nmode: rule\nproxies:\n  - name: 舞萌DX成绩更新代理\n    server: $proxyServer\n    port: $proxyPort\n    type: http\nproxy-groups:\n  - name: default\n    type: select\n    proxies:\n      - 舞萌DX成绩更新代理\nrules:\n  - DOMAIN-SUFFIX,tgk-wcaime.wahlap.com,default\n  - MATCH,DIRECT"
}