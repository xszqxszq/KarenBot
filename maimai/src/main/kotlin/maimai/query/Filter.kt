package xyz.xszq.bot.maimai.query

import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.GameVersion
import xyz.xszq.bot.maimai.music.Record

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
 * @param disable15 是否不启用 New 15
 * @param name 条件名称
 * @param fitLevelValue 是否使用拟合定数
 * @param singleChart 是否细分到谱面
 */
class Filter(
    val type: FilterType,
    val chart: (ChartInfo) -> Boolean = defaultChartFilter,
    val record: (Record) -> Boolean = defaultRecordFilter,
    val sortBy: (Record) -> Comparable<*> = defaultSort,
    val nowVersion: () -> GameVersion? = defaultVersion,
    val modifier: (Record.() -> Unit)? = null,
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