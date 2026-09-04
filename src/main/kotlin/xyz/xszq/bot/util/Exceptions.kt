package xyz.xszq.bot.util

import xyz.xszq.bot.event.MessageEvent

typealias ErrorHandler = suspend MessageEvent.(Throwable) -> Unit