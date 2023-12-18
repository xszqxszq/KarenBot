package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnimeDBItem(
    val box: List<Double>,
    val char: List<AnimeDBCharacter>,
    @SerialName("box_id")
    val boxId: String? = null
)
