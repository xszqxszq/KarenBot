package xyz.xszq.bot.admin

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import xyz.xszq.bot.KarenBotApplication
import xyz.xszq.bot.Plugin
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.log
import xyz.xszq.bot.message.Markdown
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.payload.MsgType
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.reply
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理插件
 *
 * 提供管理员命令（日志开关、插件/配置重载、消息发送与撤回）与管理员校验通道
 */
@Suppress("unused")
class Admin: Plugin() {
    lateinit var config: AdminConfig
    private val pendingSubscribes = ConcurrentHashMap<String, String>()
    private val pendingMessages = ConcurrentHashMap<String, MutableList<MessageChain>>()
    @OptIn(ExperimentalHoplite::class)
    override suspend fun load() {
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/admin.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<AdminConfig>()
        setRoute()

        logger.info { "[管理] 插件加载完成。" }
    }
    override suspend fun unload() {
        pendingSubscribes.values.forEach { id ->
            pluginLoader.subscribes.stop(id)
        }
    }
    fun MessageEvent.isAdmin() = sender.id in config.admins

    /**
     * 缓存发送失败的消息，在该群下一条消息到来时补发
     */
    private fun queuePending(openid: String, chain: MessageChain) {
        pendingMessages.getOrPut(openid) { mutableListOf() }.add(chain)
        val subscribesAt = UUID.randomUUID().toString()
        pendingSubscribes[openid] = subscribesAt
        pluginLoader.subscribes.always(subscribesAt) {
            if (this is GroupMessageEvent && group.id == openid) {
                seq = 99
                pendingMessages.remove(openid) ?.forEach { msg ->
                    reply(msg)
                }
                pluginLoader.subscribes.stop(subscribesAt)
                pendingSubscribes.remove(openid)
            }
        }
    }
    suspend fun setRoute() = route {
        // 其他插件校验管理员
        channel<AdminCheckRequest>("admin-check") { data ->
            data.deferred.complete(data.userId in config.admins)
        }
        // 调试日志开关
        startsWith("log") {
            if (isAdmin()) {
                KarenBotApplication.debugLog = !KarenBotApplication.debugLog
                reply("调试日志已${if (KarenBotApplication.debugLog) "开启" else "关闭"}。")
            }
        }
        // 重载所有插件
        startsWith("reload") { name ->
            if (isAdmin()) {
                handleReload(name)
            }
        }
        // 向指定群发送 Markdown 消息
        startsWith("msgmd") { raw ->
            if (isAdmin()) {
                val (openid, content) = raw.split(" ", limit = 2)
                val sent = bot.api.sendGroupMessage(
                    group = openid,
                    content = " ",
                    msgType = MsgType.MARKDOWN,
                    markdown = MarkdownData(content.trim()),
                    msgSeq = 99
                )
                val markdown = content.trim()
                if (sent) {
                    log(MessageChain(content))
                } else {
                    queuePending(openid, MessageChain(Markdown.create {
                        this.content = markdown
                    }))
                }
            }
        }
        // 向指定群发送文本消息
        startsWith("msg") { raw ->
            if (isAdmin()) {
                val (openid, content) = raw.split(" ", limit = 2)
                val sent = bot.api.sendGroupMessage(
                    group = openid,
                    content = content,
                    msgType = MsgType.TEXT,
                    msgSeq = 99
                )
                if (sent) {
                    log(MessageChain(content))
                } else {
                    queuePending(openid, MessageChain(content))
                }
            }
        }
        // 调试 Markdown 消息发送
        startsWith("markdown") { content ->
            if (isAdmin()) {
                val markdown = content.trim()
                reply(Markdown.create {
                    this.content = markdown
                })
            }
        }
        // 撤回指定群指定消息 ID 的消息
        startsWith("recall") { raw ->
            if (isAdmin()) {
                val (groupId, messageId) = raw.split(" ", limit = 2)
                if (bot.api.recallGroupMessage(groupId, messageId))
                    reply("撤回成功")
                else
                    reply("撤回失败")
            }
        }
    }

    private suspend fun MessageEvent.handleReload(
        name: String
    ) = when {
        name.isBlank() -> {
            pluginLoader.reloadAllPlugins()
            reply("重载所有插件完成。")
        }
        name == "config" -> {
            kotlin.runCatching {
                KarenBotApplication.reloadConfig(pluginLoader)
            }.onSuccess {
                reply("重载配置完成。")
            }.onFailure {
                reply("重载配置失败。")
                throw it
            }
        }
        else -> {
            File(pluginLoader.pluginDirectory).listFiles()?.firstOrNull {
                it.extension == "jar" && name in it.nameWithoutExtension
            }?.let {
                pluginLoader.loadOrUpdatePlugin(it, true)
                reply("重载插件完成。")
            } ?: run {
                reply("未找到相应插件。")
            }
        }
    }
}