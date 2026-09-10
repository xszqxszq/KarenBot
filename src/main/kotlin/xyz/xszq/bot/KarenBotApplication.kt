package xyz.xszq.bot

import kotlinx.coroutines.runBlocking
import xyz.xszq.bot.bootstrap.RuntimePaths

/**
 * 应用入口点
 */
object KarenBotApplication {
    @JvmStatic
    @Suppress("unused")
    fun main(args: Array<String>) = runBlocking {
        RuntimePaths.relaunchIfNeeded(KarenBotApplication::class.java.name, args)
        BotRuntime().start()
    }
}