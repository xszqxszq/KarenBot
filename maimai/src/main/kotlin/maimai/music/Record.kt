package xyz.xszq.bot.maimai.music

/**
 * 谱面成绩
 */
class Record(
    val music: MusicInfo,
    val chart: ChartInfo,
    var achievement: Int,
    var comboStatus: ComboStatus,
    var syncStatus: SyncStatus,
    val deluxeScore: Int,
    var rate: String,
    var rating: Int
)