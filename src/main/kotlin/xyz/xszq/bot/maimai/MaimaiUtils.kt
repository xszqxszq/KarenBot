package xyz.xszq.bot.maimai

import com.soywiz.kmem.toIntFloor
import com.soywiz.korim.color.RGBA
import xyz.xszq.bot.maimai.payload.MusicInfo
import xyz.xszq.bot.maimai.payload.PlayScore
import kotlin.math.min

object MaimaiUtils {
    fun ratingColor(rating: Int): String = when (rating) {
        in 0..999 -> "01"
        in 1000..1999 -> "02"
        in 2000..3999 -> "03"
        in 4000..6999 -> "04"
        in 7000..9999 -> "05"
        in 10000..11999 -> "06"
        in 12000..12999 -> "07"
        in 13000..13999 -> "08"
        in 14000..14499 -> "09"
        in 14500..14999 -> "10"
        in 15000..40000 -> "11"
        else -> "01"
    }
    fun getNewRa(ds: Double, achievement: Double): Int {
        val baseRa = when (achievement) {
            in 0.0..49.9999 ->  7.0
            in 50.0..59.9999 -> 8.0
            in 60.0..69.9999 -> 9.6
            in 70.0..74.9999 -> 11.2
            in 75.0..79.9999 -> 12.0
            in 80.0..89.9999 -> 13.6
            in 90.0..93.9999 -> 15.2
            in 94.0..96.9999 -> 16.8
            in 97.0..97.9999 -> 20.0
            in 98.0..98.9999 -> 20.3
            in 99.0..99.4999 -> 20.8
            in 99.5..99.9999 -> 21.1
            in 100.0..100.4999 -> 21.6
            in 100.5..101.0 -> 22.4
            else -> 0.0
        }
        return (ds * (min(100.5, achievement) / 100) * baseRa).toIntFloor()
    }
    fun rating2dani(rating: Int): String = when (rating) {
        0 -> "00"
        1 -> "01"
        2 -> "02"
        3 -> "03"
        4 -> "04"
        5 -> "05"
        6 -> "06"
        7 -> "07"
        8 -> "08"
        9 -> "09"
        10 -> "10"
        11 -> "12"
        12 -> "13"
        13 -> "14"
        14 -> "15"
        15 -> "16"
        16 -> "17"
        17 -> "18"
        18 -> "19"
        19 -> "20"
        20 -> "21"
        21 -> "22"
        22 -> "23"
        else -> "00"
    }
    fun genre2filename(genre: String): String = when (genre) {
        "舞萌" -> "original"
        "流行&动漫" -> "popsanime"
        "niconico & VOCALOID" -> "niconico"
        "东方Project" -> "touhou"
        "音击&中二节奏" -> "chugeki"
        "其他游戏" -> "variety"
        else -> "variety"
    }
    fun difficulty2Name(id: Int, english: Boolean = true): String {
        return if (english) {
            when (id) {
                0 -> "Bas"
                1 -> "Adv"
                2 -> "Exp"
                3 -> "Mst"
                4 -> "ReM"
                else -> ""
            }
        } else {
            when (id) {
                0 -> "绿"
                1 -> "黄"
                2 -> "红"
                3 -> "紫"
                4 -> "白"
                else -> ""
            }
        }
    }
    fun name2Difficulty(name: Char): Int? =
        when (name) {
            '绿' -> 0
            '黄' -> 1
            '红' -> 2
            '紫' -> 3
            '白' -> 4
            else -> null
        }
    fun levelIndex2Label(index: Int): String =
        when (index) {
            0 -> "Basic"
            1 -> "Advanced"
            2 -> "Expert"
            3 -> "Master"
            4 -> "Re:MASTER"
            else -> ""
        }
    fun acc2rate(acc: Double): String =
        when (acc) {
            in 0.0..49.9999 ->  "d"
            in 50.0..59.9999 -> "c"
            in 60.0..69.9999 -> "b"
            in 70.0..74.9999 -> "bb"
            in 75.0..79.9999 -> "bbb"
            in 80.0..89.9999 -> "a"
            in 90.0..93.9999 -> "aa"
            in 94.0..96.9999 -> "aaa"
            in 97.0..97.9999 -> "s"
            in 98.0..98.9999 -> "sp"
            in 99.0..99.4999 -> "ss"
            in 99.5..99.9999 -> "ssp"
            in 100.0..100.4999 -> "sss"
            in 100.5..101.0 -> "sssp"
            else -> ""
        }
    fun calcB50Change(b50: MutableList<PlayScore>,
                      music: MusicInfo, difficulty: Int, acc: Double,
                      b35Floor: Int, b15Floor: Int) : Int {
        val ra = getNewRa(music.ds[difficulty], acc)
        b50.find { it.songId.toString() == music.id && it.levelIndex == difficulty } ?.let { score ->
            val scoreRa = getNewRa(score.ds, score.achievements)
            if (ra > scoreRa)
                return ra - scoreRa
            else
                return 0
        }
        if (music.basicInfo.isNew && ra > b15Floor)
            return ra - b15Floor
        if (!music.basicInfo.isNew && ra > b35Floor)
            return ra - b35Floor
        return 0
    }
    fun toDXId(id: String): String = when (id.length) {
        5 -> id
        4 -> "1$id"
        3 -> "10$id"
        2 -> "100$id"
        else -> id
    }
    val difficulty2Color = listOf(
        RGBA(124, 216, 79), RGBA(245, 187, 11), RGBA(255, 128, 140),
        RGBA(178, 91, 245), RGBA(244, 212, 255)
    )
    val levels = listOf("1", "2", "3", "4", "5", "6", "7", "7+", "8", "8+", "9", "9+", "10", "10+", "11", "11+", "12",
        "12+", "13", "13+", "14", "14+", "15")
    val versionsBrief = listOf("真", "超", "檄", "橙", "晓", "桃", "樱", "紫", "堇", "白", "雪", "辉", "舞", "熊", "华", "爽",
    "煌", "宙", "星", "")
    val plateCategories = listOf("极", "将", "神", "舞舞", "霸者")
    val recordCategories = listOf("s", "s+", "ss", "ss+", "sss", "sss+", "ap", "ap+", "fc", "fc+", "fs", "fs+", "fdx", "fdx+",
        "clear")
    val difficulties = arrayOf("绿", "黄", "红", "紫", "白")
    val plateExcluded = listOf("201", "332", "336", "367", "486", "645")
    val remasterExcluded = listOf("158", "139", "571", "25", "72", "135", "257", "131", "351", "503", "168",
        "299", "159", "269", "138", "275", "67", "92", "108", "85", "173", "339", "369", "49")
}