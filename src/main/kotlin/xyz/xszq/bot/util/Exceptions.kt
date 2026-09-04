package xyz.xszq.bot.util

import xyz.xszq.bot.event.MessageEvent

/**
 * 异常处理
 */
typealias ErrorHandler = suspend MessageEvent.(Throwable) -> Unit