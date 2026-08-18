package xyz.xszq.bot.chunithm.exception

import xyz.xszq.bot.chunithm.api.ChunithmAPI

class NoDataException(
    message: String ?= null,
    val api: ChunithmAPI
): Exception(message)