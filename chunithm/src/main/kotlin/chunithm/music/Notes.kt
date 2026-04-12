package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
class Notes(
    val tap: Int = 0,
    val hold: Int = 0,
    val slide: Int = 0,
    val air: Int = 0,
    val flick: Int = 0
) {
    val total: Int
        get() = tap + hold + slide + air + flick
}