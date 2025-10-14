package xyz.xszq.bot.music

class Record(
    val music: MusicInfo,
    val chart: ChartInfo,
    val achievement: Int,
    val comboStatus: ComboStatus,
    val syncStatus: SyncStatus,
    val deluxeScore: Int,
    val rate: String,
    var rating: Int
)