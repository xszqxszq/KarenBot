package xyz.xszq.bot.chunithm.exception

/**
 * 用户未绑定查分器的异常
 *
 * 用户尚未绑定账号时抛出
 */
class UserBindRequiredException(
    message: String ?= null
): Exception(message)