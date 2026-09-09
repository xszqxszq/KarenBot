package xyz.xszq.bot.maimai.music

/**
 * 歌曲别名
 *
 * @property musicId 歌曲 ID
 * @property alias 别名
 * @property id 别名 ID
 */
data class MusicNameAlias(
    val musicId: Int,
    val alias: String,
    val id: String = "$musicId#$alias"
)