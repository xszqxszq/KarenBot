package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * QQ 服务器的错误响应
 */
@Serializable
data class ErrorResponse(
    val code: Int,
    val message: String
)