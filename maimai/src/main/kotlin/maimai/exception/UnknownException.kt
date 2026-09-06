package xyz.xszq.bot.maimai.exception

class UnknownException(
    message: String ?= null,
    cause: Throwable ?= null
): Exception(message, cause)