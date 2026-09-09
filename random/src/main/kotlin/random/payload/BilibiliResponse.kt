package xyz.xszq.bot.random.payload

import kotlinx.serialization.Serializable

/**
 * B站接口响应
 *
 * @property data 业务数据
 */
@Serializable
data class BilibiliResponse<T>(
    val code: Int,
    val message: String ?= null,
    val ttl: Int,
    val data: T ?= null
)