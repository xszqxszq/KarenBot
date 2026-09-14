package xyz.xszq.bot.chunithm.query

import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.Record

/**
 * 查询条件
 *
 * 同类型条件直接为或，不同条件直接为与
 *
 * @param type 条件类型
 * @param chart 谱面匹配
 * @param record 成绩匹配
 * @param sortBy 成绩排序
 * @param nowVersion 改条件下的最新版本
 * @param modifier 修改成绩
 * @param disableN20 是否为全曲要求条件
 * @param name 条件名称
 * @param singleChart 是否细分到谱面
 */
class Filter(
    val type: FilterType,
    val chart: (ChartInfo) -> Boolean = defaultChartFilter,
    val record: (Record) -> Boolean = defaultRecordFilter,
    val sortBy: (Record) -> Comparable<*> = defaultSort,
    val nowVersion: () -> GameVersion ?= defaultVersion,
    val modifier: (Record.() -> Unit) ?= null,
    val disableN20: Boolean ?= null,
    val name: String ?= null,
    val singleChart: Boolean = false
) {
    companion object {
        val defaultChartFilter: (ChartInfo) -> Boolean = { true }
        val defaultRecordFilter: (Record) -> Boolean = { true }
        val defaultSort: (Record) -> Int = { (-it.rating * 100).toInt() }
        val defaultVersion: () -> GameVersion? = { null }
    }
}