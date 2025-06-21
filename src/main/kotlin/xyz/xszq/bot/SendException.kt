package xyz.xszq.bot

import xyz.xszq.bot.payload.ErrorResponse

class SendException(
    val response: ErrorResponse,
    message: String = ""
): Exception(message)