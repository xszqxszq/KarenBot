package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.InteractionData

@Serializable
data class InteractionCreate(
    val id: String,
    val type: Int,
    val scene: String,
    @SerialName("chat_type")
    val chatType: Int,
    val timestamp: String,
    val data: InteractionData,
    @SerialName("user_openid")
    val userOpenId: String ?= null,
    @SerialName("group_openid")
    val groupOpenId: String ?= null,
    @SerialName("group_member_openid")
    val groupMemberOpenId: String ?= null
)
