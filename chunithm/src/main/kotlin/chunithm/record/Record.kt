package xyz.xszq.bot.chunithm.record

import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.MusicInfo

class Record(
    val music: MusicInfo,
    val chart: ChartInfo,
    val achievement: Int,
    val comboStatus: ComboStatus,
    val chainStatus: ChainStatus,

)