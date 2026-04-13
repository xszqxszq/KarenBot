package xyz.xszq.bot.chunithm.music

sealed interface Response {
    val player: PlayerInfo
    var settings: PlayerSettings?
}