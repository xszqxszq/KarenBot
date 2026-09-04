package xyz.xszq.bot.exception

/**
 * 命令参数不足的异常
 */
class ArgsNotEnoughException(
    message: String = ""
): Exception(message)