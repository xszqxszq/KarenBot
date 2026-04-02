package xyz.xszq.bot.controller

import xyz.xszq.bot.Maimai

sealed class Controller(
    open val maimai: Maimai
) {
    abstract suspend fun setRoute()
    open suspend fun unload() {}
}