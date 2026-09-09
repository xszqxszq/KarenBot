package xyz.xszq.bot.exception

/**
 * 需要重试的异常
 */
class RetryException(
    message: String = ""
): Exception(message)