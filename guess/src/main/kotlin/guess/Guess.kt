package xyz.xszq.bot.guess

import xyz.xszq.bot.Plugin
import xyz.xszq.bot.guess.touhou.Touhou

@Suppress("unused")
class Guess: Plugin() {
    val touhou = Touhou(this)
    override suspend fun load() {
        touhou.init()

        setRoute()
        logger.info { "[猜谜] 插件加载完成。" }
    }

    suspend fun setRoute() {
        touhou.setRoute()
    }
}