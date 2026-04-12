package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
enum class MusicGenre(val id: Int, val genreName: String, val aliases: List<String>) {
    PopsAnime(0, "流行 & 动漫",
        listOf("动漫", "流行", "二次元")),
    Niconico(2, "niconico",
        listOf("nico", "v家", "v", "术力口", "术", "ボカロ", "ボーカロイド", "ニコニコ", "ニコ")),
    Touhou(3, "东方Project",
        listOf("东方", "东", "车", "東方")),
    Original(5, "原创",
        listOf("中二", "chunithm")),
    Variety(6, "其他游戏",
        listOf("其他", "variety")),
    IrodoriMidori(7, "彩绿",
        listOf("イロドリミドリ")),
    GekiMai(9, "音击舞萌",
        listOf("舞萌音击", "ゲキマイ", "gekimai", "maigeki"));

    companion object {
        fun of(genreName: String): MusicGenre = MusicGenre.entries.first { it.genreName == genreName }
    }
}