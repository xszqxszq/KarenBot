package xyz.xszq.bot.chunithm.music

data class MusicNameAlias(
    val musicId: Int,
    val alias: String,
    val id: String = "$musicId#$alias"
)