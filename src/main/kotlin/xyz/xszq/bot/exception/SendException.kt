package xyz.xszq.bot.exception

import xyz.xszq.bot.payload.ErrorResponse

class SendException(
    val response: ErrorResponse,
    message: String = ""
): Exception(message)