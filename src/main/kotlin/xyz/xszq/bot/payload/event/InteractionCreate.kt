package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.InteractionData

/**
 * 互动事件
 *
 * @property chatType 会话类型
 * @property data 数据
 * @property userOpenId 触发用户的 OpenID
 * @property groupOpenId 群聊的 OpenID
 * @property groupMemberOpenId 触发群成员的 OpenID
 */
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