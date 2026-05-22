package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.xszq.bot.config.ForwardConfig
import xyz.xszq.bot.event.*
import xyz.xszq.bot.message.FileManager
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.payload.event.*
import java.security.SecureRandom
import javax.net.ssl.SSLContext

/**
 * Configure routing for Webhook server.
 */
fun Application.configureRouting(
    logger: KLogger,
    pluginLoader: PluginLoader,
    filter: WordFilter
) {
    routing {
        get("/") {
            call.respondText { "200 OK" }
        }
        post("/webhook") {
            /* Validate Headers */
            if (call.request.headers["User-Agent"] != "QQBot-Callback"
                || call.request.headers["X-Bot-Appid"] != pluginLoader.api.config.appId)
                return@post call.respond(HttpStatusCode.BadRequest)
            val signature = call.request.headers["X-Signature-Ed25519"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            val timestamp = call.request.headers["X-Signature-Timestamp"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)

            /* Verify body's integrity */
            val body = call.receiveText()
            if (!verifyBody(pluginLoader.api.config.clientSecret, signature, timestamp, body))
                return@post call.respond(HttpStatusCode.Unauthorized)

            /* Parse Webhook payload */
            kotlin.runCatching {
                val payload = json.decodeFromString<Payload>(body)
                when (payload.op) {
                    /* Normal Message */
                    OpCode.DISPATCH -> {
                        KarenBotApplication.forwardConfig?.let { forwardConfig ->
                            handleForward(payload, call, logger, filter, pluginLoader, forwardConfig, body)
                        } ?: run {
                            handleDispatch(payload, call, logger, filter, pluginLoader)
                        }
                    }
                    /* Webhook Validation */
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
                    /* Unknown code */
                    else -> {
                        call.respond(HttpStatusCode.NotAcceptable)
                    }
                }
            }.onFailure {
                /* Unhandled Exception */
                it.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}

suspend fun Application.downloadFiles(
    attachments: List<Attachment>,
    fileManager: FileManager,
    logger: KLogger
) = attachments.mapNotNull { attachment ->
    downloadFile(attachment.url, attachment.filename, logger)
}.also {
    launch(Dispatchers.IO) {
        fileManager.addFiles(it)
    }
}

/**
 * Handle OpCode.DISPATCH.
 * @param payload Event Payload.
 * @param call Route call.
 * @param logger The Logger.
 * @param pluginLoader The Plugin Loader.
 */
@OptIn(DelicateCoroutinesApi::class)
suspend fun Application.handleDispatch(
    payload: Payload,
    call: RoutingCall,
    logger: KLogger,
    filter: WordFilter,
    pluginLoader: PluginLoader
) {
    call.respond(Payload(OpCode.HTTP_CALLBACK_ACK))
    when (payload.t) {
        /* 私聊消息 */
        EventType.C2C.Message -> {
            val data = json.decodeFromString<C2CMessageCreate>(payload.d!!)
            val images = downloadFiles(
                data.attachments.filter { "image" in it.contentType }, pluginLoader.files, logger)
            val files = downloadFiles(
                data.attachments.filter { it.contentType == "file" }, pluginLoader.files, logger)
            val content = filter.filter(data.content)
            val message = MessageChain(content, images, files)
            logger.info {
                "${data.author.username}(${data.author.id}) -> ${message.content.trim().replace("\n", "\\n")}"
            }
            MessageEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                id = data.id,
                message = message,
                sender = User(pluginLoader.bot, data.author.id, data.author.username)
            )
        }
        /* 群聊AT消息 */
        EventType.Group.AtMessage -> {
            val data = json.decodeFromString<GroupAtMessageCreate>(payload.d!!)
            val images = downloadFiles(
                data.attachments.filter { "image" in it.contentType }, pluginLoader.files, logger)
            val files = downloadFiles(
                data.attachments.filter { it.contentType == "file" }, pluginLoader.files, logger)
            val content = filter.filter(data.content)
            val message = MessageChain(content, images, files, ark = data.arkData)
            logger.info {
                "[${data.group}] ${data.author.username}(${data.author.id}) -> ${message.content.trim().replace("\n", "\\n")}"
            }
            GroupMessageEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                id = data.id,
                message = message,
                sender = User(pluginLoader.bot, data.author.id, data.author.username),
                group = Group(pluginLoader.bot, data.group),
                reference = data.messageElements.firstOrNull() ?.toMessageChain(),
                // TODO: Replace to Authentic bot data
                mentions = listOf(User(pluginLoader.bot, "", "", isBot = true, isSelf = true))
            )
        }
        /* 群聊消息 */
        EventType.Group.Message -> {
            val data = json.decodeFromString<GroupMessageCreate>(payload.d!!)
            println(payload.d)
            val images = downloadFiles(
                data.attachments.filter { "image" in it.contentType }, pluginLoader.files, logger)
            val files = downloadFiles(
                data.attachments.filter { it.contentType == "file" }, pluginLoader.files, logger)
            val content = filter.filter(data.content)
            val message = MessageChain(content, images, files, pluginLoader.bot, data.mentions, ark = data.arkData)
            logger.info {
                "[${data.group}] ${data.author.username}(${data.author.id}) -> ${message.content.trim().replace("\n", "\\n")}"
            }
            GroupMessageEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                id = data.id,
                message = message,
                sender = User(pluginLoader.bot, data.author.id, data.author.username),
                group = Group(pluginLoader.bot, data.group),
                reference = data.messageElements.firstOrNull() ?.toMessageChain(),
                mentions = data.mentions.filter { it.scope != "all" }.map { mention ->
                    User(pluginLoader.bot, mention.id, mention.username,
                        isBot = mention.bot, isSelf = mention.isSelf)
                }
            )
        }
        /* 新增好友 */
        EventType.C2C.Add -> {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.info {
                "Friend Added: ${data.user}"
            }
            BotAddFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        /* 新增群聊 */
        EventType.Group.Add -> {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.info {
                "Group Added: ${data.group} (by ${data.operator})"
            }
            BotJoinGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = User(pluginLoader.bot, data.operator)
            )
        }
        /* 好友移除 */
        EventType.C2C.Remove -> {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.info {
                "Friend Deleted: ${data.user}"
            }
            BotRemoveFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        /* 群聊移除 */
        EventType.Group.Remove -> {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.info {
                "Group Deleted: ${data.group} (by ${data.operator})"
            }
            BotLeaveGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = User(pluginLoader.bot, data.operator)
            )
        }
        /* 允许私聊消息推送(已下线) */
        EventType.C2C.Receive -> {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.info {
                "Friend receive message: ${data.user}"
            }
            BotReceiveFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        /* 允许群组消息推送(已下线) */
        EventType.Group.Receive -> {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.info {
                "Group receive message: ${data.group} (by ${data.operator})"
            }
            BotReceiveGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = User(pluginLoader.bot, data.operator)
            )
        }
        /* 拒绝私聊消息推送(已下线) */
        EventType.C2C.Reject -> {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.info {
                "Friend reject message: ${data.user}"
            }
            BotRejectFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        /* 拒绝群组消息推送(已下线) */
        EventType.Group.Reject -> {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.info {
                "Group reject message: ${data.group} (by ${data.operator})"
            }
            BotRejectGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = User(pluginLoader.bot, data.operator)
            )
        }
        /* 互动消息 */
        EventType.Interaction -> {
            val data = json.decodeFromString<InteractionCreate>(payload.d!!)
            if (data.data.resolved.buttonData == null || data.data.resolved.buttonId == null) {
                logger.warn { "未知的 Interaction：${data}" }
                return
            }
            when (data.chatType) {
                ChatType.GROUP -> GroupInteractionEvent(
                    bot = pluginLoader.bot,
                    eventId = payload.id!!,
                    id = "",
                    data = data.data.resolved.buttonData,
                    button = data.data.resolved.buttonId,
                    sender = User(pluginLoader.bot, data.groupMemberOpenId!!),
                    group = Group(pluginLoader.bot, data.groupOpenId!!)
                ).also {
                    logger.info {
                        "[互动] [${data.groupOpenId}] (${data.groupMemberOpenId}) " +
                                "<${data.data.resolved.buttonId}> ${data.data.resolved.buttonData}"
                    }
                }
                ChatType.C2C -> InteractionEvent(
                    bot = pluginLoader.bot,
                    eventId = payload.id!!,
                    id = "",
                    data = data.data.resolved.buttonData,
                    button = data.data.resolved.buttonId,
                    sender = User(pluginLoader.bot, data.userOpenId!!)
                ).also {
                    logger.info {
                        "[互动] [${data.userOpenId}] " +
                                "<${data.data.resolved.buttonId}> ${data.data.resolved.buttonData.replace("\n", "\\n")}"
                    }
                }
                else -> null
            }
        }
        /* 无法处理的事件 */
        else -> null
    } ?.let { event ->
        launch(Dispatchers.IO) {
            pluginLoader.subscribes.handle(event)
        }
    }
}

/**
 * Handle Forward message.
 * @param payload Event Payload.
 * @param call Route call.
 * @param logger The Logger.
 * @param pluginLoader The Plugin Loader.
 */
suspend fun Application.handleForward(
    payload: Payload,
    call: RoutingCall,
    logger: KLogger,
    filter: WordFilter,
    pluginLoader: PluginLoader,
    forwardConfig: ForwardConfig,
    body: String
) {
    call.respond(Payload(OpCode.HTTP_CALLBACK_ACK))
    when (payload.t) {
        /* Private message */
        EventType.C2C.Message -> {
            val data = json.decodeFromString<C2CMessageCreate>(payload.d!!)
            if (forwardConfig.debug)
                println("Received: [${data.author.id}]")
            when {
                data.author.id in forwardConfig.subjects -> forward(body, call, forwardConfig.whitelist)
                forwardConfig.otherwise.isNotBlank() -> forward(body, call, forwardConfig.otherwise)
                else -> handleDispatch(payload, call, logger, filter, pluginLoader)
            }
        }
        /* Group @at message */
        EventType.Group.AtMessage -> {
            val data = json.decodeFromString<GroupAtMessageCreate>(payload.d!!)
            if (forwardConfig.debug)
                println("Received: [${data.group}] (${data.author.id})")
            when {
                data.group in forwardConfig.groups -> forward(body, call, forwardConfig.whitelist)
                forwardConfig.otherwise.isNotBlank() -> forward(body, call, forwardConfig.otherwise)
                else -> handleDispatch(payload, call, logger, filter, pluginLoader)
            }
        }
        /* Group message */
        EventType.Group.Message -> {
            val data = json.decodeFromString<GroupMessageCreate>(payload.d!!)
            if (forwardConfig.debug)
                println("Received: [${data.group}] (${data.author.id})")
            when {
                data.group in forwardConfig.groups -> forward(body, call, forwardConfig.whitelist)
                forwardConfig.otherwise.isNotBlank() -> forward(body, call, forwardConfig.otherwise)
                else -> handleDispatch(payload, call, logger, filter, pluginLoader)
            }
        }
        EventType.Interaction -> {
            val data = json.decodeFromString<InteractionCreate>(payload.d!!)
            when {
                data.chatType == ChatType.GROUP && data.groupOpenId in forwardConfig.groups ->
                    forward(body, call, forwardConfig.whitelist)
                forwardConfig.otherwise.isNotBlank() -> forward(body, call, forwardConfig.otherwise)
                else -> handleDispatch(payload, call, logger, filter, pluginLoader)
            }
        }
        else -> {
            if (forwardConfig.otherwise.isNotBlank())
                forward(body, call, forwardConfig.otherwise)
            else
                handleDispatch(payload, call, logger, filter, pluginLoader)
        }
    }
}
val forwardClient = HttpClient(OkHttp) {
    engine {
        config {
            sslSocketFactory(
                SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf(AllTrustManager()), SecureRandom())
                }.socketFactory,
                AllTrustManager()
            )
            hostnameVerifier { _, _ -> true }
        }
    }
}
val forwardHeaders = listOf("User-Agent", "X-Bot-Appid", "X-Signature-Ed25519", "X-Signature-Timestamp")
suspend fun forward(
    body: String,
    call: RoutingCall,
    to: String
) {
    val requested = call.request.headers
    forwardClient.post(to) {
        forwardHeaders.forEach { header ->
            header(header, requested[header] ?: "")
        }
        setBody(body)
    }
}
suspend fun PluginLoader.manualTrigger(event: Event) {
    subscribes.handle(event)
}