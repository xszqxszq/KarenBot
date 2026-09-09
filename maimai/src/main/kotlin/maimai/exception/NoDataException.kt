package xyz.xszq.bot.maimai.exception

import xyz.xszq.bot.maimai.api.MaimaiAPI

/**
 * 玩家没有成绩数据的异常
 *
 * 成绩列表为空，玩家尚未向查分器导入成绩时抛出
 *
 * @property api 对应的查分器后端
 */
class NoDataException(
    message: String ?= null,
    val api: MaimaiAPI
): Exception(message)