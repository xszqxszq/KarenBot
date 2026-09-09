package xyz.xszq.bot.maimai.exception

/**
 * 不支持的操作的异常
 *
 * 使用了另一查分器特有功能的异常
 */
class NotSupportedException(
    message: String
): Exception(message)