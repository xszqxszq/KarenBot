package xyz.xszq.bot.maimai.music

import kotlinx.serialization.Serializable

/**
 * 谱面物量
 */
@Serializable
data class Notes(
    val tap: Int,
    val hold: Int,
    val slide: Int,
    val touch: Int,
    val `break`: Int,
) {
    /**
     * 总物量
     */
    val total: Int
        get() = tap + hold + slide + touch + `break`

    /**
     * 最高 DX 分数
     */
    val maxDeluxeScore: Int
        get() = total * 3

    constructor() : this(0, 0, 0, 0, 0)
}