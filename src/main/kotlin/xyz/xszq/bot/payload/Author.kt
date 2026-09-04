package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 单聊消息的作者
 */
@Serializable
data class Author(
    @SerialName("user_openid")
    val id: String,
    val username: String = "",
    val bot: Boolean = false
)