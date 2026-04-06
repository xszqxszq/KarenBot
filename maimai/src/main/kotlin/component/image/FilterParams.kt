package xyz.xszq.bot.component.image

import xyz.xszq.bot.music.GameVersion
import xyz.xszq.bot.music.RequiresType

data class FilterParams(
    var name: String,
    var newestVersion: GameVersion,
    var isAllRequired: Boolean,
    var isFitLevelValue: Boolean,
    var isDetailed: Boolean,
    var requiresType: RequiresType = RequiresType.Achievement,
)