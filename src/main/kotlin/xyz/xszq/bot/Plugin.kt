package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import xyz.xszq.bot.subscribe.SubscribeBuilder

/**
 * Base class of every Plugin.
 */
abstract class Plugin {
    lateinit var plugin: String
    lateinit var pluginLoader: PluginLoader
    val logger = KotlinLogging.logger {}

    /**
     * This executes when Plugin is loaded.
     */
    open fun load() {
        logger.info { "[插件] 已加载插件: $plugin" }
    }

    /**
     * This executes when Plugin is unloaded.
     */
    open fun unload() {

    }

    /**
     * Create route of all commands.
     * @param prefix Prefix of all commands.
     * @param force Force check prefix.
     * @param block Block of code to build Subscribes.
     */
    fun route(prefix: String? = null, force: Boolean = false, block: SubscribeBuilder.() -> Unit) {
        block(SubscribeBuilder(
            plugin = plugin, prefix = prefix, forcePrefix = force, manager = pluginLoader.subscribes))
    }
}