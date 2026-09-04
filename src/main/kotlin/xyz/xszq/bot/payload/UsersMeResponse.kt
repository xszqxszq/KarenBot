package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bot 自身详情
 */
@Serializable
data class UsersMeResponse(
    val id: String = "",
    val username: String = "",
    val avatar: String = "",
    @SerialName("share_url")
    val shareUrl: String = "",
    @SerialName("welcome_msg")
    val welcomeMsg: String = "",
    @SerialName("welcome_suggestions")
    val welcomeSuggestions: List<String> = listOf()
)