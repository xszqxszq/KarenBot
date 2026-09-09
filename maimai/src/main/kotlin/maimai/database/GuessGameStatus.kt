package xyz.xszq.bot.maimai.database

import kotlinx.serialization.Serializable

/**
 * 猜歌游戏状态
 */
@Serializable
sealed class GuessGameStatus {
    /**
     * 经典模式
     *
     * @property musicId 歌曲 ID
     * @property hints 提示文本
     */
    @Serializable
    data class Classical(
        val musicId: Int,
        val hints: List<String>? = null
    ): GuessGameStatus()
    /**
     * 开字母模式
     *
     * @property musics 歌曲 ID 及开出状态
     * @property opened 已开出的字母列表
     */
    @Serializable
    data class Opening(
        val musics: List<Pair<Int, Boolean>>,
        val opened: List<Char>
    ): GuessGameStatus()
}