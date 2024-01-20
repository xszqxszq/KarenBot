package xyz.xszq.bot.rhythmgame.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChartNotes(
    val tap: Int,
    val hold: Int,
    val slide: Int,
    @SerialName("break_")
    val breaks: Int,
    val touch: Int ?= null) {
    companion object {
        fun fromList(notes: List<Int>): ChartNotes? = when (notes.size) {
            4 -> ChartNotes(notes[0], notes[1], notes[2], notes[3], null)
            5 -> ChartNotes(notes[0], notes[1], notes[2], notes[4], notes[3]) // Interesting result array
            else -> null
        }
    }
}