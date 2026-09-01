package xyz.xszq.bot.maimai.exception

class UserQueriedNoBindingException(
    message: String = "您查询的用户未绑定水鱼账户，无法查询"
): Exception(message)