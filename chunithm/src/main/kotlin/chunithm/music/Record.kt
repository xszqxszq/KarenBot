package xyz.xszq.bot.chunithm.music

class Record(
    val music: MusicInfo,
    val chart: ChartInfo,
    var achievement: Int,
    val comboStatus: ComboStatus,
    val chainStatus: ChainStatus = ChainStatus.None,
    val clear: String = "",
    var rating: Double = 0.0,
    var rate: String = Rate[achievement]
)