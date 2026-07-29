package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.maimai.music.GameVersion
import xyz.xszq.bot.maimai.music.Record
import xyz.xszq.bot.maimai.music.RequiresType

data class FilterParams(
    var name: String = "",
    var newestVersion: GameVersion,
    var isAllRequired: Boolean,
    var isFitLevelValue: Boolean,
    var isDetailed: Boolean,
    var requiresType: RequiresType = RequiresType.Achievement,
    var sortBy: List<(Record) -> Comparable<*>> = emptyList()
)