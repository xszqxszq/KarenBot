package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import xyz.xszq.bot.event.MessageEvent
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
        startsWith("reload") { name ->
            if (isAdmin()) {
                handleReload(name)
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
