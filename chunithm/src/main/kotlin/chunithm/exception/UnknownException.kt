package xyz.xszq.bot.chunithm.exception

/**
 * 未知错误的异常
 *
 * 请求返回未预期的状态码或兜底逻辑失败时抛出
 */
class UnknownException(
    message: String ?= null,
    cause: Throwable ?= null
): Exception(message, cause)