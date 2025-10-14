package xyz.xszq.bot.controller

import xyz.xszq.bot.Maimai

sealed class Controller(
    open val maimai: Maimai
) {
    abstract fun setRoute()
}