package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 落雪 API 的通用响应
 *
 * @property success 请求是否成功
 * @property data 业务数据，成功时非空
 * @property message 失败时描述原因
 */
@Serializable
data class LXNSResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String? = null,
    val data: T? = null
)