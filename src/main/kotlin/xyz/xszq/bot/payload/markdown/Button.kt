package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Button(
    val id: String,
    @SerialName("render_data")
    val renderData: RenderData,
    val action: Action
)