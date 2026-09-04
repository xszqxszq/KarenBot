package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.ArkData
import xyz.xszq.bot.payload.Attachment
import xyz.xszq.bot.payload.Member
import xyz.xszq.bot.payload.ReferenceMessage

/**
 * 群聊消息创建事件
 *
 * @property author 消息发送成员
 * @property group 消息所在群的 OpenID
 * @property content 消息文本内容
 * @property mentions 消息中被@的成员列表
 * @property messageElements 消息引用的历史消息元素
 */
@Serializable
data class GroupMessageCreate(
    val id: String,
    val author: Member,
    val content: String,
    val timestamp: String,
    @SerialName("group_openid")
    val group: String,
    val attachments: List<Attachment> = listOf(),
    val mentions: List<Member> = listOf(),
    @SerialName("msg_elements")
    val messageElements: List<ReferenceMessage> = listOf(),
    @SerialName("ark_data")
    val arkData: ArkData? = null
)