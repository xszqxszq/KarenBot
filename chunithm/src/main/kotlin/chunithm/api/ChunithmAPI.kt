package xyz.xszq.bot.chunithm.api

import xyz.xszq.bot.chunithm.record.UserQuery
import xyz.xszq.bot.chunithm.record.UserRating

/**
 * 查分器后端统一接口
 */
interface ChunithmAPI {
    // 查分器ID
    val id: String

    // 查分器名称
    val name: String

    /**
     * 初始化加载调用
     */
    suspend fun load()

    /**
     * 获取用户信息
     */
    suspend fun getPlayerRating(query: UserQuery): UserRating
}