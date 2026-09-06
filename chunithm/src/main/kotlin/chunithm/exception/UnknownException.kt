package xyz.xszq.bot.chunithm.exception

class UnknownException(
    message: String ?= null,
    cause: Throwable ?= null
): Exception(message, cause)