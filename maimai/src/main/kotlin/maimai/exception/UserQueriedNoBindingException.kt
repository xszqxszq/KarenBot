package xyz.xszq.bot.maimai.exception

/**
 * 被查询用户未绑定的异常
 *
 * 查询其他玩家而对方未绑定查分器时抛出
 */
class UserQueriedNoBindingException(
    message: String = "您查询的用户未绑定水鱼账户，无法查询"
): Exception(message)