package xyz.xszq.bot.maimai.exception

/**
 * 筛选结果过多的异常
 *
 * 按筛选条件查询命中的曲目数量过多时抛出
 */
class FilterTooManyException(
    message: String ?= null
): Exception(message)