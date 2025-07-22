package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeImageId(
    @SerialName("image_id")
    val id: String
)
