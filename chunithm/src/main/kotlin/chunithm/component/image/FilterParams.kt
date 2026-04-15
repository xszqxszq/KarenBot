package xyz.xszq.bot.chunithm.component.image

import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.Record
import xyz.xszq.bot.chunithm.music.RequiresType

data class FilterParams(
    var name: String = "",
    var newestVersion: GameVersion,
    var isAllRequired: Boolean,
    var isDetailed: Boolean,
    var requiresType: RequiresType = RequiresType.Achievement,
    var sortBy: List<(Record) -> Comparable<*>> = emptyList()
)