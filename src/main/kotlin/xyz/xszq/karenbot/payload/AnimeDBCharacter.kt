package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeDBCharacter(
    val name: String,
    @SerialName("cartoonname")
    val from: String,
    @SerialName("acc")
    val accuracy: Double
)
