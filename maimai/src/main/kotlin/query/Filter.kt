package xyz.xszq.bot.query

import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.GameVersion
import xyz.xszq.bot.music.Record

class Filter(
    val type: FilterType,
    val chart: (ChartInfo) -> Boolean = defaultChartFilter,
    val record: (Record) -> Boolean = defaultRecordFilter,
    val sortBy: (Record) -> Comparable<*> = defaultSort,
    val nowVersion: () -> GameVersion? = defaultVersion,
    val disable15: Boolean = false,
    val name: String? = null,
    val fitLevelValue: Boolean = false,
    val singleChart: Boolean = false
) {
    companion object {
        val defaultChartFilter: (ChartInfo) -> Boolean = { true }
        val defaultRecordFilter: (Record) -> Boolean = { true }
        val defaultSort: (Record) -> Int = { -it.rating }
        val defaultVersion: () -> GameVersion? = { null }
    }
}