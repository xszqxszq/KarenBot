package xyz.xszq.bot.maimai.music

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MusicGenre(val id: Int, val genreName: String, val value: String, val names: Array<String>) {
    @SerialName("POPSアニメ")
    PopsAnime(101, "流行&动漫", "POPSアニメ",
        arrayOf("动漫", "流行", "二次元")),
    @SerialName("niconicoボーカロイド")
    Niconico(102, "niconico＆VOCALOID™", "niconicoボーカロイド",
        arrayOf("nico", "v家", "v", "术力口", "术", "ボカロ", "ボーカロイド", "ニコニコ", "ニコ")),
    @SerialName("東方Project")
    Touhou(103, "东方Project", "東方Project",
        arrayOf("东方", "东", "车", "東方")),
    @SerialName("ゲームバラエティ")
    Variety(104, "其他游戏", "ゲームバラエティ",
        arrayOf("其他", "variety")),
    @SerialName("maimai")
    Original(105, "舞萌", "maimai",
        arrayOf("舞萌", "maimai")),
    @SerialName("オンゲキCHUNITHM")
    Chugeki(106, "音击&中二节奏", "オンゲキCHUNITHM",
        arrayOf("音击中二", "中二音击", "chugeki", "gekichu")),
    @SerialName("宴会場")
    Utage(107, "宴会場", "宴会場",
        arrayOf("宴会场"));

    companion object {
        fun of(value: String): MusicGenre = MusicGenre.entries.first { it.value == value }
    }
}