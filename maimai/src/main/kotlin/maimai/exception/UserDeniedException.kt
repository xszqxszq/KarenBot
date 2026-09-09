package xyz.xszq.bot.maimai.exception

/**
 * 用户拒绝授权的异常
 *
 * 用户设置了隐私限制时抛出
 */
class UserDeniedException(
    message: String ?= null
): Exception(message)