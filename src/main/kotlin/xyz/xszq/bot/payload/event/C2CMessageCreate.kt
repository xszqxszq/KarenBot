package xyz.xszq.bot.payload.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xszq.bot.payload.ArkData
import xyz.xszq.bot.payload.Attachment
import xyz.xszq.bot.payload.Author

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
