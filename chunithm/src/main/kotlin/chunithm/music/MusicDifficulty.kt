package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
enum class MusicDifficulty(val id: Int, val aliases: List<String>) {
    Basic(0, listOf("绿谱", "绿")),
    Advanced(1, listOf("黄谱", "黄")),
    Expert(2, listOf("红谱", "红")),
    Master(3, listOf("紫谱", "紫")),
    Ultima(4, listOf("黑谱", "黑")),
    WorldsEnd(5, listOf("we谱", "we", "世界末日", "彩"));

    companion object {
        fun of(id: Int): MusicDifficulty =
            MusicDifficulty.entries.first { it.id == id }
        fun from(alias: String): MusicDifficulty? =
            MusicDifficulty.entries.firstOrNull { alias in it.aliases }
    }
}