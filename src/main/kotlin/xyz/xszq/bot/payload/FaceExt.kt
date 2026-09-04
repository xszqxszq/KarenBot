package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * QQ 表情额外信息
 */
@Serializable
data class FaceExt(
    val text: String,
    val num: Int = 0
)