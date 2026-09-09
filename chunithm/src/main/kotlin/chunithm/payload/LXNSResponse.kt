package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的通用响应
 *
 * @property success 请求是否成功
 * @property data 业务数据
 * @property message 失败时描述原因
 */
@Serializable
data class LXNSResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String? = null,
    val data: T? = null
)