package xyz.xszq.bot.maimai.database

import kotlinx.serialization.Serializable

@Serializable
sealed class GuessGameStatus {
    @Serializable
    data class Classical(
        val musicId: Int,
        val hints: List<String>? = null
    ): GuessGameStatus()
    @Serializable
    data class Opening(
        val musics: List<Pair<Int, Boolean>>,
        val opened: List<Char>
    ): GuessGameStatus()
}