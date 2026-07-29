package xyz.xszq.bot.maimai.music

sealed interface Response {
    val player: PlayerInfo
    var settings: PlayerSettings?
}