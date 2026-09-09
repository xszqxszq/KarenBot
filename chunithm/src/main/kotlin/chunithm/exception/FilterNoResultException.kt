package xyz.xszq.bot.chunithm.exception

/**
 * 筛选无结果的异常
 *
 * 按筛选条件查询歌曲未命中任何结果时抛出
 */
class FilterNoResultException(
    message: String ?= null
): Exception(message)