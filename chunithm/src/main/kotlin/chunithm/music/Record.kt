package xyz.xszq.bot.chunithm.music

class Record(
    val music: MusicInfo,
    val chart: ChartInfo,
    val score: Int,
    val comboStatus: ComboStatus,
    val chainStatus: ChainStatus = ChainStatus.None,
    val clear: String = "",
    val rank: String = "",
    val rating: Double = 0.0
)