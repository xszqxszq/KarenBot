package xyz.xszq.bot.chunithm.query

import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.Record

class Filter(
    val type: FilterType,
    val chart: (ChartInfo) -> Boolean = defaultChartFilter,
    val record: (Record) -> Boolean = defaultRecordFilter,
    val sortBy: (Record) -> Comparable<*> = defaultSort,
    val nowVersion: () -> GameVersion? = defaultVersion,
    val disableN20: Boolean = false,
    val name: String? = null,
    val singleChart: Boolean = false
) {
    companion object {
        val defaultChartFilter: (ChartInfo) -> Boolean = { true }
        val defaultRecordFilter: (Record) -> Boolean = { true }
        val defaultSort: (Record) -> Int = { (-it.rating * 100).toInt() }
        val defaultVersion: () -> GameVersion? = { null }
    }
}