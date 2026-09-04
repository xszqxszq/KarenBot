package xyz.xszq.bot.webhook

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.xszq.bot.*
import xyz.xszq.bot.event.*
import xyz.xszq.bot.message.FileManager
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.message.RemoteImage
import xyz.xszq.bot.payload.Attachment
import xyz.xszq.bot.payload.ChatType
import xyz.xszq.bot.payload.EventType
import xyz.xszq.bot.payload.Payload
import xyz.xszq.bot.payload.event.*
import xyz.xszq.bot.util.downloadFile
import xyz.xszq.bot.util.recvGroupAtLogger
import xyz.xszq.bot.util.recvGroupLogger
import xyz.xszq.bot.util.recvGroupBotLogger
import xyz.xszq.bot.util.recvC2CLogger
import xyz.xszq.bot.util.eventLogger
import xyz.xszq.bot.service.WordFilter
import xyz.xszq.bot.util.json

/**
 * Webhook 事件分发
 *
 * @property forwarder 事件转发器
 */
class WebhookDispatcher(
    private val forwarder: WebhookForwarder
) {
    /**
     * 下载消息中的文件，并存入文件管理器
     *
     * @param attachments 文件列表
     * @param fileManager 文件管理器
     * @param logger 日志器
     * @return 已下载消息文件及信息
     */
    private suspend fun Application.downloadFiles(
        attachments: List<Attachment>,
        fileManager: FileManager,
        logger: KLogger
    ) = attachments.mapNotNull { attachment ->
        downloadFile(attachment.url, attachment.filename, logger) ?.let { file ->
            Triple(
                attachment.url,
                file,
                RemoteImage(
                    url = attachment.url,
                    filename = attachment.filename,
                    contentType = attachment.contentType,
                    width = attachment.width ?: 0,
                    height = attachment.height ?: 0
                )
            )
        }
    }.also { pairs ->
        launch(Dispatchers.IO) {
            fileManager.addFiles(pairs.map { it.second })
        }
    }

    /**
     * 下载消息中的图片
     *
     * @param attachments 文件列表
     * @param fileManager 文件管理器
     * @param logger 日志器
     * @return 已下载图片及其信息
     */
    private suspend fun Application.downloadImages(
        attachments: List<Attachment>,
        fileManager: FileManager,
        logger: KLogger
    ) = downloadFiles(
        attachments = attachments.filter { "image" in it.contentType },
        fileManager = fileManager,
        logger = logger
    )

    private fun memberOf(
        bot: Bot,
        id: String,
        username: String,
        role: String
    ) = Member(
        bot = bot,
        id = id,
        username = username,
        role = MemberRole.of(role)
    )

    /**
     * Webhook 事件分发
     *
     * @param payload 收到的负荷
     * @param call 收到的请求
     * @param logger 日志器
     * @param filter 敏感词过滤器
     * @param pluginLoader 插件加载器
     * @param body 原始请求
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun Application.dispatch(
        payload: Payload,
        call: RoutingCall,
        logger: KLogger,
        filter: WordFilter,
        pluginLoader: PluginLoader,
        body: String
    ): Job? = when (payload.t) {
        // 私聊消息
        EventType.C2C.Message -> run {
            val data = json.decodeFromString<C2CMessageCreate>(payload.d!!)
            logger.debug { "Received: [${data.author.id}]" }
            if (forwardTo(subject = data.author.id, body = body, call = call))
                return@run null
            val images = downloadImages(data.attachments, pluginLoader.files, logger)
            val content = filter.filter(data.content)
            val message = MessageChain(content, images, attachments = data.attachments)
            recvC2CLogger.info {
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
        // 群聊消息与 AT 消息
        EventType.Group.AtMessage, EventType.Group.Message -> run {
            val data = json.decodeFromString<GroupMessageCreate>(payload.d!!)
            logger.debug { "Received: [${data.group}] (${data.author.id})" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            val images = downloadImages(data.attachments, pluginLoader.files, logger)
            val content = filter.filter(data.content)
            val isAt = payload.t == EventType.Group.AtMessage
            val message = when {
                isAt -> MessageChain(
                    raw = content,
                    images = images,
                    ark = data.arkData,
                    attachments = data.attachments
                )
                else -> MessageChain(
                    raw = content,
                    images = images,
                    bot = pluginLoader.bot,
                    mentions = data.mentions,
                    ark = data.arkData,
                    attachments = data.attachments
                )
            }
            when {
                isAt -> recvGroupAtLogger.info {
                    "[${data.group}] ${data.author.username}(${data.author.id}) -> " +
                            message.content.trim().replace("\n", "\\n")
                }
                data.author.bot -> recvGroupBotLogger.info {
                    "[${data.group}] ${data.author.username}(${data.author.id}) -> " +
                            message.content.trim().replace("\n", "\\n")
                }
                else -> recvGroupLogger.info { "[${data.group}] ${data.author.username}(${data.author.id}) -> " +
                        message.content.trim().replace("\n", "\\n")
                }
            }
            if (!isAt && (data.author.bot || data.mentions.any { it.bot && !it.isSelf }))
                return@run null
            GroupMessageEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                id = data.id,
                message = message,
                sender = memberOf(pluginLoader.bot, data.author.id, data.author.username, data.author.role),
                group = Group(pluginLoader.bot, data.group),
                reference = data.messageElements.firstOrNull() ?.toMessageChain(),
                mentions = when {
                    isAt -> listOf(pluginLoader.bot.self)
                    else -> data.mentions.filter { it.scope != "all" }.map { mention ->
                        User(pluginLoader.bot, mention.id, mention.username,
                            isBot = mention.bot, isSelf = mention.isSelf)
                    }
                }
            )
        }
        // 新增好友
        EventType.C2C.Add -> run {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.debug { "Received: Friend ${data.user} Added" }
            if (forwardTo(subject = data.user, body = body, call = call))
                return@run null
            eventLogger.info { "添加好友: ${data.user}" }
            BotAddFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        // 新增群聊
        EventType.Group.Add -> run {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.debug { "Received: Joined group [${data.group}] by (${data.operator})" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            eventLogger.info { "加入群聊: ${data.group} (by ${data.operator})" }
            BotJoinGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = Member(pluginLoader.bot, data.operator)
            )
        }
        // 好友移除
        EventType.C2C.Remove -> run {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.debug { "Received: Friend ${data.user} Deleted" }
            if (forwardTo(subject = data.user, body = body, call = call))
                return@run null
            eventLogger.info { "删除好友: ${data.user}" }
            BotRemoveFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        // 群聊移除
        EventType.Group.Remove -> run {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.debug { "Received: Left group [${data.group}] by (${data.operator})" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            eventLogger.info { "退出群聊: ${data.group} (by ${data.operator})" }
            BotLeaveGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = Member(pluginLoader.bot, data.operator)
            )
        }
        // 允许私聊主动消息
        EventType.C2C.Receive -> run {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.debug { "Received: User ${data.user} allowed message push" }
            if (forwardTo(subject = data.user, body = body, call = call))
                return@run null
            eventLogger.info { "用户允许主动消息: ${data.user}" }
            BotReceiveFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        // 允许群组主动消息
        EventType.Group.Receive -> run {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.debug { "Received: Group [${data.group}] allowed message push by (${data.operator})" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            eventLogger.info { "群聊允许主动消息: ${data.group} (by ${data.operator})" }
            BotReceiveGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = Member(pluginLoader.bot, data.operator)
            )
        }
        // 拒绝私聊主动消息
        EventType.C2C.Reject -> run {
            val data = json.decodeFromString<C2CBotUpdate>(payload.d!!)
            logger.debug { "Received: User ${data.user} denied message push" }
            if (forwardTo(subject = data.user, body = body, call = call))
                return@run null
            eventLogger.info { "用户关闭主动消息: ${data.user}" }
            BotRejectFriendEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                user = User(pluginLoader.bot, data.user)
            )
        }
        // 拒绝群组主动消息
        EventType.Group.Reject -> run {
            val data = json.decodeFromString<GroupBotUpdate>(payload.d!!)
            logger.debug { "Received: Group [${data.group}] denied message push by (${data.operator})" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            eventLogger.info { "群聊关闭主动消息: ${data.group} (by ${data.operator})" }
            BotRejectGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                operator = Member(pluginLoader.bot, data.operator)
            )
        }
        // 群有新成员加入
        EventType.Group.MemberAdd -> run {
            val data = json.decodeFromString<GroupMemberUpdate>(payload.d!!)
            logger.debug { "Received: Group [${data.group}] has member (${data.member}) joined" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            UserJoinGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                user = Member(pluginLoader.bot, data.member)
            )
        }
        // 群有成员退群
        EventType.Group.MemberRemove -> run {
            val data = json.decodeFromString<GroupMemberUpdate>(payload.d!!)
            logger.debug { "Received: Group [${data.group}] has member (${data.member}) left" }
            if (forwardTo(group = data.group, body = body, call = call))
                return@run null
            UserLeaveGroupEvent(
                bot = pluginLoader.bot,
                eventId = payload.id!!,
                group = Group(pluginLoader.bot, data.group),
                user = Member(pluginLoader.bot, data.member)
            )
        }
        // 互动消息
        EventType.Interaction -> run {
            val data = json.decodeFromString<InteractionCreate>(payload.d!!)
            logger.debug { "Received: Interaction [${data.groupOpenId ?: "C2C"}] (${data.userOpenId})" }
            if (forwardTo(group = data.groupOpenId, body = body, call = call))
                return@run null
            if (data.data.resolved.buttonData == null || data.data.resolved.buttonId == null) {
                logger.warn { "未知的 Interaction：${data}" }
                null
            } else when (data.chatType) {
                ChatType.GROUP -> GroupInteractionEvent(
                    bot = pluginLoader.bot,
                    eventId = payload.id!!,
                    id = "",
                    data = data.data.resolved.buttonData,
                    button = data.data.resolved.buttonId,
                    sender = Member(pluginLoader.bot, data.groupMemberOpenId!!),
                    group = Group(pluginLoader.bot, data.groupOpenId!!)
                ).also {
                    eventLogger.info {
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
                    eventLogger.info {
                        val value = data.data.resolved.buttonData.replace("\n", "\\n")
                        "[互动] [${data.userOpenId}] " +
                                "<${data.data.resolved.buttonId}> $value"
                    }
                }
                else -> null
            }
        }
        // 无法处理的事件
        else -> null
    } ?.let { delivered ->
        launch(Dispatchers.IO) {
            pluginLoader.subscribes.handle(delivered)
        }
    }

    /**
     * 命中转发规则时，进行转发
     *
     * @param subject 私聊 ID，仅私聊类事件传入
     * @param group 群 ID，仅群聊类事件传入
     * @param body 原始请求体
     * @param call 收到的请求
     * @return 是否成功转发
     */
    private suspend fun forwardTo(
        subject: String ?= null,
        group: String ?= null,
        body: String,
        call: RoutingCall
    ): Boolean {
        val target = forwarder.forwardTarget(subject = subject, group = group) ?: return false
        forwarder.forward(body, call, target)
        return true
    }
}