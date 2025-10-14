package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable
import xyz.xszq.bot.music.Notes

@Serializable
data class LocalChartInfo(
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String
)
