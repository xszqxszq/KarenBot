package xyz.xszq.bot.exception

import xyz.xszq.bot.api.MaimaiAPI

class NoDataException(
    message: String ?= null,
    val api: MaimaiAPI
): Exception(message)
