package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 本地段位扣血详情
 */
@Serializable
data class LocalCourseDamage(
    val perfect: Int,
    val great: Int,
    val good: Int,
    val miss: Int
)