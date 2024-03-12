package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
enum class LevelIndex(val value: Int) {
    BASIC(0), ADVANCED(1), EXPERT(2), MASTER(3), ULTIMA(4)
}