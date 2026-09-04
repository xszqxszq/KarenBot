package xyz.xszq.bot.exception

import xyz.xszq.bot.payload.ErrorResponse

/**
 * 发送消息失败的异常
 */
class SendException(
    val response: ErrorResponse,
    message: String = ""
): Exception(message)