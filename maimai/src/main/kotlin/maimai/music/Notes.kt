package xyz.xszq.bot.maimai.music

import kotlinx.serialization.Serializable

@Serializable
data class Notes(
    val tap: Int,
    val hold: Int,
    val slide: Int,
    val touch: Int,
    val `break`: Int,
) {
    val total: Int
        get() = tap + hold + slide + touch + `break`
    val maxDeluxeScore: Int
        get() = total * 3
    constructor() : this(0, 0, 0, 0, 0)
}