package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.maimai.music.GameVersion
import xyz.xszq.bot.maimai.music.Record
import xyz.xszq.bot.maimai.music.RequiresType

/**
 * 随心配渲染参数
 *
 * @property name 查询命令
 * @property isAllRequired 是否不启用 New 15
 * @property isFitLevelValue 是否使用拟合定数
 * @property isDetailed 是否按定数值分组
 */
data class FilterParams(
    var name: String = "",
    var newestVersion: GameVersion,
    var isAllRequired: Boolean,
    var isFitLevelValue: Boolean,
    var isDetailed: Boolean,
    var requiresType: RequiresType = RequiresType.Achievement,
    var sortBy: List<(Record) -> Comparable<*>> = emptyList()
)