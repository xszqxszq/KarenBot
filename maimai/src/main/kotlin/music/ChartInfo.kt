package xyz.xszq.bot.music

class ChartInfo(
    val music: MusicInfo,
    val difficulty: MusicDifficulty,
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String
) {
    val maxDeluxeScore: Int
        get() = notes.maxDeluxeScore
}
