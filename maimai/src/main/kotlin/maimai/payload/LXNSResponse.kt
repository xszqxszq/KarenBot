package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSResponse<T>(
    val success: Boolean,
    val code: Int,
    val message: String? = null,
    val data: T? = null
)
