package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
class ChartInfo(
    val musicInfo: MusicInfo,
    val difficulty: MusicDifficulty,
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String,
    val kanji: String ?= null,
    val star: Int ?= null
) {
    var origin: MusicInfo ?= null
}