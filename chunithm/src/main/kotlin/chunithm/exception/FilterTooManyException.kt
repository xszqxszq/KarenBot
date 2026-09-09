package xyz.xszq.bot.chunithm.exception

/**
 * 筛选结果过多的异常
 *
 * 按筛选条件查询命中的歌曲数量过多时抛出
 */
class FilterTooManyException(
    message: String ?= null
): Exception(message)