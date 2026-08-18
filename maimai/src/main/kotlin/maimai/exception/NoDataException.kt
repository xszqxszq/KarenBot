package xyz.xszq.bot.maimai.exception

import xyz.xszq.bot.maimai.api.MaimaiAPI

class NoDataException(
    message: String ?= null,
    val api: MaimaiAPI
): Exception(message)