package xyz.xszq.bot.rhythmgame.maimai

import korlibs.io.lang.substr
import korlibs.math.roundDecimalPlaces
import korlibs.memory.toIntFloor
import xyz.xszq.bot.rhythmgame.maimai.payload.MusicInfo
import xyz.xszq.bot.rhythmgame.maimai.payload.PlayScore
import kotlin.math.min

object MaimaiUtils {
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
    val dailyOps = listOf(
        "推分", "下埋", "越级", "拼机", "单刷", "练底力", "练手法", "抓准度", "抓绝赞", "收歌", "堵门", "夜勤"
    )
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
        val baseRa = when (achievement.roundDecimalPlaces(4)) {
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
            in 99.5..99.9998 -> 21.1
            99.9999 -> 21.4
            in 100.0..100.4998 -> 21.6
            100.4999 -> 22.2
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
    fun toDXId(id: String): String = when (id.length) {
        5 -> id
        4 -> "1$id"
        3 -> "10$id"
        2 -> "100$id"
        else -> id
    }
    fun getPlateVerList(version: String) = when (version) {
        "真" -> listOf("maimai", "maimai PLUS")
        "超" -> listOf("maimai GreeN")
        "檄" -> listOf("maimai GreeN PLUS")
        "橙" -> listOf("maimai ORANGE")
        in listOf("晓", "暁") -> listOf("maimai ORANGE PLUS")
        "桃" -> listOf("maimai PiNK")
        "樱" -> listOf("maimai PiNK PLUS")
        "紫" -> listOf("maimai MURASAKi")
        in listOf("堇", "菫") -> listOf("maimai MURASAKi PLUS")
        "白" -> listOf("maimai MiLK")
        "雪" -> listOf("MiLK PLUS")
        in listOf("辉", "輝") -> listOf("maimai FiNALE")
        in listOf("熊", "华") -> listOf("maimai でらっくす", "maimai でらっくす PLUS")
        in listOf("爽", "煌") -> listOf("maimai でらっくす Splash")
        in listOf("宙", "星") -> listOf("maimai でらっくす UNiVERSE")
        in listOf("舞", "") -> listOf("maimai", "maimai PLUS", "maimai GreeN", "maimai GreeN PLUS", "maimai ORANGE",
            "maimai ORANGE PLUS", "maimai PiNK", "maimai PiNK PLUS", "maimai MURASAKi", "maimai MURASAKi PLUS",
            "maimai MiLK", "MiLK PLUS", "maimai FiNALE")
        "all" -> listOf("maimai", "maimai PLUS", "maimai GreeN", "maimai GreeN PLUS", "maimai ORANGE",
            "maimai ORANGE PLUS", "maimai PiNK", "maimai PiNK PLUS", "maimai MURASAKi", "maimai MURASAKi PLUS",
            "maimai MiLK", "MiLK PLUS", "maimai FiNALE", "maimai でらっくす", "maimai でらっくす PLUS",
            "maimai でらっくす Splash", "maimai でらっくす Splash PLUS", "maimai でらっくす UNiVERSE",
            "maimai でらっくす UNiVERSE PLUS", "maimai でらっくす FESTiVAL", "maimai でらっくす FESTiVAL PLUS")
        else -> emptyList()
    }
    private val plateStartOffset = buildMap {
        put("真", 6101)
        put("超", 6104)
        put("檄", 6108)
        put("橙", 6112)
        put("暁", 6116)
        put("晓", 6116)
        put("桃", 6120)
        put("櫻", 6124)
        put("樱", 6124)
        put("紫", 6128)
        put("菫", 6132)
        put("堇", 6132)
        put("白", 6136)
        put("雪", 6140)
        put("輝", 6144)
        put("辉", 6144)
        put("舞", 6149)
        put("熊", 55101)
        put("華", 109101)
        put("华", 109101)
        put("爽", 159101)
        put("煌", 209101)
        put("宙", 259101)
        put("星", 309101)
        put("祭", 359101)
    }
    fun getPlateFilename(plate: String): String? {
        if (plate == "霸者")
            return "UI_Plate_006148.png"

        val fileId = when {
            plate.length == 2 -> when (plate[1]) {
                in listOf('極', '极') -> 0
                '将' -> 1
                '神' -> 2
                else -> return null
            }
            plate.length == 3 && plate.substr(1) == "舞舞" -> 3
            else -> return null
        } + (plateStartOffset[plate[0].toString()] ?: return null)
        return "UI_Plate_${fileId.toString().padStart(6, '0')}.png"
    }
    fun getPlateByFilename(filename: String): Pair<String, String>? {
        versionsBrief.forEach { ver ->
            plateCategories.forEach { type ->
                if (ver != "" || type == "霸者") {
                    val now = getPlateFilename(ver + type)
                    if (now == filename) {
                        return Pair(ver, type)
                    }
                }
            }
        }
        return null
    }
    fun plateVerToVerId(ver: String) = when (ver) {
        "真" -> "100"
        "超" -> "120"
        "檄" -> "130"
        "橙" -> "140"
        in listOf("晓", "暁") -> "150"
        "桃" -> "160"
        "樱" -> "170"
        "紫" -> "180"
        in listOf("堇", "菫") -> "185"
        "白" -> "190"
        "雪" -> "195"
        in listOf("辉", "輝") -> "199"
        "熊" -> "200"
        "华" -> "210"
        "爽" -> "214"
        "煌" -> "215"
        "宙" -> "220"
        "星" -> "225"
        "祭" -> "230"
        else -> ""
    }
}