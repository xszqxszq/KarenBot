package xyz.xszq.nereides

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import xyz.xszq.config
import xyz.xszq.nereides.event.*
import xyz.xszq.nereides.payload.event.GroupAddBot
import xyz.xszq.nereides.payload.event.GroupAtMessageCreate
import xyz.xszq.nereides.payload.event.GroupDelBot
import xyz.xszq.nereides.payload.event.GuildAtMessageCreate
import xyz.xszq.nereides.payload.user.GuildUser
import xyz.xszq.nereides.payload.utils.AccessTokenRequest
import xyz.xszq.nereides.payload.utils.AccessTokenResponse
import xyz.xszq.nereides.payload.utils.WSSGatewayBotResponse
import xyz.xszq.nereides.payload.utils.WSSGatewayResponse
import xyz.xszq.nereides.payload.websocket.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

open class QQClient(
    private val appId: String,
    private val clientSecret: String,
    private val easyToken: String,
    private val sandbox: Boolean
): CoroutineScope, C2CApi, GuildApi {
    var bot: Bot by Delegates.notNull()
    var restarting: Boolean = false
    private val server = "https://api.sgroup.qq.com"
    private val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    private val wsClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    val client = HttpClient(Apache5) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    override val logger = KotlinLogging.logger("QQClient")
    override suspend fun call(method: HttpMethod, api: String, payload: Any?): HttpResponse {
        return kotlin.runCatching {
            client.request("${server}${api}") {
                this.method = method
                contentType(ContentType.Application.Json)
                payload?.let {
                    setBody(it)
                }
                headers {
                    setHeaders()
                }
            }
        }.onFailure {
            it.printStackTrace()
        }.getOrThrow()
    }
    override suspend fun get(api: String): HttpResponse =
        call(HttpMethod.Get, api, null)
    override suspend fun post(api: String, payload: Any): HttpResponse =
        call(HttpMethod.Post, api, payload).body()
    override suspend fun multipart(
        api: String,
        values: Map<String, String>,
        file: Pair<String, Pair<String, ByteArray>>
    ): HttpResponse {
        return client.submitForm(
            "${server}${api}",
        ) {
            method = HttpMethod.Post
            headers {
                contentType(ContentType.MultiPart.FormData)
                setHeaders()
            }
            setBody(MultiPartFormDataContent(
                formData {
                    values.forEach { (key, value) ->
                        append(key, value)
                    }
                    append(
                        file.first,
                        file.second.second,
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=${file.second.first}")
                        }
                    )
                }
            ))
        }
    }

    override val coroutineContext: CoroutineContext = Job()

    private var token: String? = null
    private var tokenExpireTime by Delegates.notNull<Long>()
    private var heartbeatInterval by Delegates.notNull<Long>()
    private var sessionId by Delegates.notNull<String>()
    private var seq = AtomicInteger(1)
    lateinit var botGuildInfo: GuildUser

    private fun HeadersBuilder.setHeaders() = runBlocking {
        set("Authorization", "QQBot ${getToken()}")
        set("X-Union-Appid", appId)
    }

    private suspend fun getToken(): String {
        if (token == null || tokenExpireTime - System.currentTimeMillis() <= 60 * 1000L) {
            refreshToken()
        }
        return token!!
    }
    private suspend fun refreshToken() {
        val response: AccessTokenResponse = client.post("https://bots.qq.com/app/getAppAccessToken") {
            contentType(ContentType.Application.Json)
            setBody(AccessTokenRequest(appId, clientSecret))
        }.body()
        token = response.accessToken
        tokenExpireTime = System.currentTimeMillis() + response.expiresIn * 1000L
    }

    private suspend fun getWSSGateway(): String {
        return call(HttpMethod.Get, "/gateway", null).body<WSSGatewayResponse>().url
    }
    private suspend fun getShards(): WSSGatewayBotResponse {
        return call(HttpMethod.Get, "/gateway/bot", null).body<WSSGatewayBotResponse>()
    }

    private suspend fun DefaultClientWebSocketSession.identify() {
        sendSerialized(
            PayloadSend(
            OpCode.Identify,
            IdentifyRequest(
                "$appId.$easyToken",
                Intents.GUILDS or // TODO: Need to be able to customize
                        Intents.GUILD_MEMBERS or
                        Intents.GUILD_MESSAGE_REACTIONS or
                        Intents.PUBLIC_GUILD_MESSAGES or
                        Intents.C2C or
                        Intents.INTERACTION,
                listOf(config.shardId, config.shardTotal)
            )
        )
        )
    }
    private suspend fun DefaultClientWebSocketSession.resume() {
        sendSerialized(
            PayloadSend(
                OpCode.Resume,
                ResumeRequest(
                    "$appId.$easyToken",
                    sessionId,
                    seq.get()
                )
            )
        )
    }
    private fun DefaultClientWebSocketSession.launchHeartbeatTask() = launch {
        while (true) {
            sendSerialized(
                PayloadSend(
                    OpCode.Heartbeat,
                    seq.get()
                )
            )
            delay(heartbeatInterval)
        }
    }
    suspend fun listen() {
        var resume = false
        var heartbeat: Job by Delegates.notNull()
        val handlePayload: suspend DefaultClientWebSocketSession.(Frame,Boolean) -> Unit = { frame, resume ->
            val payload = json.decodeFromString<PayloadRecv>((frame as Frame.Text).readText())
            logger.debug { payload }
            when (payload.op) {
                OpCode.Hello -> {
                    val data = json.decodeFromString<HelloResponse>(payload.d!!)
                    heartbeatInterval = data.heartbeatInterval
                    if (resume)
                        resume()
                    else
                        identify()
                    botGuildInfo = getBotInfo()
                    heartbeat = launchHeartbeatTask()
                }
                OpCode.Reconnect -> {
                    heartbeat.cancel()
                    this.close()
                }
                OpCode.InvalidSession -> {
                    logger.error { "建立连接时连接出错，请重启！" }
                }
                OpCode.Dispatch -> {
                    try {
                        handleEvent(payload)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                OpCode.HeartbeatACK -> {
                    logger.debug { "收到心跳包响应" }
                }
                else -> {

                }
            }
        }
        while (true) {
            try {
                wsClient.webSocket(getWSSGateway()) {
                    incoming.consumeEach {
                        launch {
                            handlePayload.invoke(this@webSocket, it, resume)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            logger.warn { "Websocket 连接已断开，重连中……" }
            resume = true
            delay(200L)
        }
    }

    private suspend fun handleEvent(payload: PayloadRecv) {
        payload.s ?.let {
            seq.set(it)
        }
        if (payload.t != "GROUP_AT_MESSAGE_CREATE")
            logger.info { "收到事件 ${payload.t}" }
        when (payload.t) {
            "READY" -> {
                val data = json.decodeFromString<ReadyResponse>(payload.d!!)
                sessionId = data.sessionId
                GlobalEventChannel.broadcast(BotReadyEvent(bot, data.user, data.shard))
                logger.info { "已成功建立 Websocket 连接。" }
            }
            "RESUMED" -> {
                GlobalEventChannel.broadcast(BotReconnectEvent(bot))
                logger.info { "已恢复连接。" }
            }
            "GROUP_AT_MESSAGE_CREATE" -> {
                val data = json.decodeFromString<GroupAtMessageCreate>(payload.d!!)
                logger.info { "[${data.groupId}] ${data.author.id} -> ${data.content.trim()}" }
                GlobalEventChannel.broadcast(
                    GroupAtMessageEvent(
                        bot = bot,
                        msgId = data.id,
                        groupId = data.groupId,
                        subjectId = data.author.id,
                        message = parseContent(data),
                        timestamp = parseDate(data.timestamp)
                    ))
            }
            "AT_MESSAGE_CREATE" -> {
                val data = json.decodeFromString<GuildAtMessageCreate>(payload.d!!)
                logger.info { "[${data.channelId}] ${data.author.username} -> ${data.content.trim()}" }
                GlobalEventChannel.broadcast(
                    GuildAtMessageEvent(
                        bot = bot,
                        msgId = data.id,
                        channelId = data.channelId,
                        guildId = data.guildId,
                        subjectId = data.author.id,
                        message = parseContent(data),
                        timestamp = parseDate(data.timestamp),
                        author = data.author,
                        mentions = data.mentions
                    )
                )
            }
            "GROUP_ADD_ROBOT" -> {
                val data = json.decodeFromString<GroupAddBot>(payload.d!!)
                logger.info { "机器人加入了群 [${data.groupId}]，操作人：[${data.operator}]" }
                GlobalEventChannel.broadcast(
                    BotJoinGroupEvent(
                        bot = bot,
                        groupId = data.groupId,
                        operator = data.operator
                    )
                )
            }
            "GROUP_DEL_ROBOT" -> {
                val data = json.decodeFromString<GroupDelBot>(payload.d!!)
                logger.info { "机器人离开了群 [${data.groupId}]，操作人：[${data.operator}]" }
                GlobalEventChannel.broadcast(
                    BotJoinGroupEvent(
                        bot = bot,
                        groupId = data.groupId,
                        operator = data.operator
                    )
                )
            }
        }
    }
}