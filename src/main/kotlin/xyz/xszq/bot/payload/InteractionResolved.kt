package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InteractionResolved(
    @SerialName("button_data")
    val buttonData: String,
    @SerialName("button_id")
    val buttonId: String
)
