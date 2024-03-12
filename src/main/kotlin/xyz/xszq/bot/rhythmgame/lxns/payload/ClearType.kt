package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
enum class ClearType(val value: String) {
    Clear("clear"), Failed("failed")
}
