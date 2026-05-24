package xyz.xszq.bot.chunithm.music

import kotlinx.serialization.Serializable

@Serializable
enum class MusicGenre(val id: Int, val genreName: String, val names: Array<String>) {
    PopsAnime(0, "流行 & 动漫", arrayOf("流行 & 动漫", "流行&动漫", "动漫", "流行", "二次元", "pops & anime", "pops&anime")),
    Niconico(2, "niconico", arrayOf("niconico", "nico", "v家", "v", "术力口", "术", "ボカロ", "ボーカロイド", "ニコニコ", "ニコ")),
    Touhou(3, "东方Project", arrayOf("东方Project", "东方", "东", "车", "東方")),
    Original(5, "原创", arrayOf("原创", "中二", "chunithm")),
    Variety(6, "其他游戏", arrayOf("其他游戏", "其他", "variety", "game variety")),
    IrodoriMidori(7, "彩绿", arrayOf("彩绿", "イロドリミドリ")),
    GekiMai(9, "音击舞萌", arrayOf("音击舞萌", "舞萌音击", "ゲキマイ", "gekimai", "maigeki"));

    companion object {
        fun of(name: String): MusicGenre = MusicGenre.entries.firstOrNull { genre ->
            genre.genreName == name || genre.names.any { value -> value.equals(name, true) }
        } ?: Original
    }
}