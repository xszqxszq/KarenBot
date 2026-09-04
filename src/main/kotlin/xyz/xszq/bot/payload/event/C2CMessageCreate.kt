package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.ArkData
import xyz.xszq.bot.payload.Attachment
import xyz.xszq.bot.payload.Author

/**
 * 单聊消息事件
 *
 * @property author 消息发送者
 * @property content 消息文本内容
 * @property attachments 消息附件
 * @property arkData 卡片消息
 */
@Serializable
data class C2CMessageCreate(
    val id: String,
    val author: Author,
    val content: String,
    val timestamp: String,
    val attachments: List<Attachment> = listOf(),
    @SerialName("ark_data")
    val arkData: ArkData? = null
)