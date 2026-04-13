package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
enum class MusicDifficulty(val value: Int, val names: List<String>) {
    Basic(0, listOf("绿谱", "绿")),
    Advanced(1, listOf("黄谱", "黄")),
    Expert(2, listOf("红谱", "红")),
    Master(3, listOf("紫谱", "紫")),
    Ultima(4, listOf("黑谱", "黑")),
    WorldsEnd(5, listOf("we谱", "we", "世界末日", "彩"));

    companion object {
        fun of(value: Int): MusicDifficulty = MusicDifficulty.entries.first { it.value == value }
        fun from(name: String): MusicDifficulty? = MusicDifficulty.entries.firstOrNull { name in it.names }
    }

    val brief
        get() = names.last()
}