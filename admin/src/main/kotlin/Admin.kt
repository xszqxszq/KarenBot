package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import xyz.xszq.bot.event.GroupMessageEvent
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.message.MessageChain
import xyz.xszq.bot.payload.AdminCheckRequest
import xyz.xszq.bot.payload.MsgType
import java.io.File

@Suppress("unused")
class Admin: Plugin() {
    lateinit var config: AdminConfig
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
    fun MessageEvent.isAdmin() = sender.id in config.admins
    suspend fun setRoute() = route {
        channel<AdminCheckRequest>("admin-check") { data ->
            data.deferred.complete(data.userId in config.admins)
        }
        startsWith("reload") { name ->
            if (isAdmin()) {
                handleReload(name)
            }
        }
        startsWith("msg") { raw ->
            if (isAdmin()) {
                val (openid, content) = raw.split(" ", limit = 2)
                bot.api.sendGroupMessage(
                    group = openid,
                    content = content,
                    msgType = MsgType.TEXT
                )
                log(MessageChain(content))
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
