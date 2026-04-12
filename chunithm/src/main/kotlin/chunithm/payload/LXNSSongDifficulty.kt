package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSSongDifficulty(
    val difficulty: Int,
    val level: String,
    @SerialName("level_value")
    val levelValue: Double,
    @SerialName("note_designer")
    val noteDesigner: String,
    val version: Int,
    val notes: LXNSNotes ?= null,
    @SerialName("origin_id")
    val originId: Int ?= null,
    val kanji: String ?= null,
    val star: Int ?= null
)
