package xyz.xszq.bot

import xyz.xszq.bot.touhou.Touhou

@Suppress("unused")
class Guess: Plugin() {
    val touhou = Touhou(this)
    override fun load() {
        setRoute()
        logger.info { "[猜谜] 插件加载完成。" }
    }

    fun setRoute() {
        touhou.setRoute()
    }
}