package xyz.xszq.bot.chunithm.exception

/**
 * 用户不存在的异常
 *
 * 查分器找不到要查询的玩家时抛出
 */
class UserNotFoundException(
    message: String ?= null
): Exception(message)