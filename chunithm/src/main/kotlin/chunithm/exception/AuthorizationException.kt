package xyz.xszq.bot.chunithm.exception

/**
 * 鉴权失败的异常
 *
 * 查分器返回未授权状态或访问令牌失效时抛出
 */
class AuthorizationException(
    message: String ?= null
): Exception(message)