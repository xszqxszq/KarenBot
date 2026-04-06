package xyz.xszq.bot.music

sealed interface Response {
    val player: PlayerInfo
    var settings: PlayerSettings?
}