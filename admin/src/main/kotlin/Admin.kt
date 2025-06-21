package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.file.baseName
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.flow.firstOrNull
import xyz.xszq.bot.event.MessageEvent

@Suppress("unused")
class Admin: Plugin() {
    lateinit var config: AdminConfig
    @OptIn(ExperimentalHoplite::class)
    override fun load() {
        config = ConfigLoaderBuilder.default()
            .addFileSource("./config/admin.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<AdminConfig>()
        setRoute()

        logger.info { "[管理] 插件加载完成。" }
    }
    fun MessageEvent.isAdmin() = sender.id in config.admins
    fun setRoute() = route {
        startsWith("reload") { name ->
            if (isAdmin()) {
                if (name.isBlank()) {
                    pluginLoader.reloadAllPlugins()
                    reply("重载所有插件完成。")
                } else {
                    localCurrentDirVfs[pluginLoader.pluginDirectory].list().firstOrNull {
                        name in it.baseName
                    } ?.let {
                        pluginLoader.loadOrUpdatePlugin(it, true)
                        reply("重载插件完成。")
                    } ?: run {
                        reply("未找到相应插件。")
                    }
                }
            }
        }
    }
}