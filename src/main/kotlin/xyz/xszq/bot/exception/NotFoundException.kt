package xyz.xszq.bot.exception

/**
 * 查询结果不存在的异常
 */
class NotFoundException(
    message: String = ""
): Exception(message)