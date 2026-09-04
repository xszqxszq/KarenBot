package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.subscribe.SubscribeBuilder

/**
 * 插件基类
 */
abstract class Plugin {
    lateinit var plugin: String
    lateinit var pluginLoader: PluginLoader
    val logger = KotlinLogging.logger {}
    val database: Database
        get() = pluginLoader.database

    /**
     * 插件加载时执行
     */
    open suspend fun load() {
        logger.info { "[插件] 已加载插件: $plugin" }
    }

    /**
     * 插件卸载时执行
     */
    open suspend fun unload() {

    }

    /**
     * 注册全部命令路由
     * @param prefix 父级命令前缀
     * @param force 是否一定要父级前缀
     * @param block 订阅路由的代码块
     */
    suspend fun route(prefix: String? = null, force: Boolean = false, block: suspend SubscribeBuilder.() -> Unit) {
        block(SubscribeBuilder(
            plugin = plugin, prefix = prefix, forcePrefix = force, manager = pluginLoader.subscribes))
    }
}