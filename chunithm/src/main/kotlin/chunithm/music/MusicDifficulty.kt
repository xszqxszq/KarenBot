package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

/**
 * 谱面难度
 */
@Serializable
enum class MusicDifficulty(val value: Int, val names: Array<String>) {
    Basic(0, arrayOf("绿谱", "绿")),
    Advanced(1, arrayOf("黄谱", "黄")),
    Expert(2, arrayOf("红谱", "红")),
    Master(3, arrayOf("紫谱", "紫")),
    Ultima(4, arrayOf("黑谱", "黑")),
    WorldsEnd(5, arrayOf("we谱", "we", "世界末日", "彩"));

    companion object {
        /**
         * 根据 ID 匹配 难度
         *
         * @param value ID
         * @return 难度
         */
        fun of(value: Int): MusicDifficulty = MusicDifficulty.entries.first { it.value == value }
        /**
         * 根据名称匹配难度
         *
         * @param name 名称
         * @return 难度
         */
        fun from(name: String): MusicDifficulty? = MusicDifficulty.entries.firstOrNull { name in it.names }
    }

    /**
     * 难度简称
     */
    val brief
        get() = names.last()
}