package xyz.xszq.bot.component

import xyz.xszq.bot.event.MessageEvent

typealias ErrorHandler = suspend MessageEvent.(Throwable, String) -> Unit