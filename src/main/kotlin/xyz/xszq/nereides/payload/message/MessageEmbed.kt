package xyz.xszq.nereides.payload.message

import kotlinx.serialization.Serializable

@Serializable
data class MessageEmbed(
    val title: String,
    val prompt: String,
    val thumbnail: MessageThumbnail,
    val fields: List<MessageEmbedField>
)
