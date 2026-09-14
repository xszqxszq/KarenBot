package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 群聊基本信息
 */
@Serializable
data class GroupInfoResponse(
    @SerialName("group_openid")
    val group: String = "",
    @SerialName("group_name")
    val name: String = "",
    @SerialName("group_finger_memo")
    val description: String = "",
    @SerialName("group_class_text")
    val category: String = "",
    @SerialName("group_tags")
    val tags: List<String> = listOf(),
    @SerialName("group_member_num")
    val memberCount: Int = 0
)