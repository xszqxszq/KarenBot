package xyz.xszq.bot.chunithm.component.image

import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.Record
import xyz.xszq.bot.chunithm.music.RequiresType

/**
 * 随心配渲染参数
 *
 * @property name 查询命令
 * @property isAllRequired 是否不启用 N20
 * @property isDetailed 是否按定数值分组
 */
data class FilterParams(
    var name: String = "",
    var newestVersion: GameVersion,
    var isAllRequired: Boolean ?= null,
    var isDetailed: Boolean,
    var requiresType: RequiresType = RequiresType.Achievement,
    var sortBy: List<(Record) -> Comparable<*>> = emptyList()
)