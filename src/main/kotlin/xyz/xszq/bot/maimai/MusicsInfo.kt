package xyz.xszq.bot.maimai

import io.github.oshai.kotlinlogging.KLogger
import korlibs.math.roundDecimalPlaces
import korlibs.memory.toIntCeil
import korlibs.memory.toIntFloor
import xyz.xszq.bot.maimai.MaimaiUtils.calcB50Change
import xyz.xszq.bot.maimai.MaimaiUtils.difficulty2Name
import xyz.xszq.bot.maimai.MaimaiUtils.getNewRa
import xyz.xszq.bot.maimai.MaimaiUtils.plateExcluded
import xyz.xszq.bot.maimai.MaimaiUtils.remasterExcluded
import xyz.xszq.bot.maimai.payload.*

class MusicsInfo(val logger: KLogger) {
    private val musics = mutableMapOf<String, MusicInfo>()
    private val stats = mutableMapOf<String, List<ChartStat>>()
    private var hotList = listOf<String>()
    private var randomHotMusics = mutableListOf<MusicInfo>()
    fun updateMusicInfo(data: List<MusicInfo>) {
        musics.putAll(data.associateBy { it.id })
    }
    fun updateStats(data: HashMap<String, List<ChartStat>>) {
        stats.putAll(data)
    }
    fun getById(id: String) = musics[id]
    fun getAll(): List<MusicInfo> = musics.values.toList()
    fun getInfo(id: String): String {
        val music = musics[id] ?: return ""
        val ds = music.ds.mapIndexed { index, d ->
            if (music.level[index].endsWith("+") && d == d.toIntFloor() + 0.5)
                music.level[index]
            else
                d
        }.joinToString("/")
        return buildString {
            appendLine()
            appendLine("${music.id}. ${music.title}")
            appendLine("艺术家：${music.basicInfo.artist}")
            appendLine("分类：${music.basicInfo.genre}")
            appendLine(
                buildString {
                    append("版本：${music.basicInfo.from}")
                    if (music.basicInfo.isNew)
                        append(" （计入b15）")
                }
            )
            appendLine(
                buildString {
                    append("BPM：")
                    if (music.basicInfo.bpm < 0)
                        append("未知")
                    else
                        append(music.basicInfo.bpm)
                }
            )
            appendLine("定数：$ds")
        }
    }
    fun getInfoWithDifficulty(id: String, difficulty: Int): String {
        musics[id] ?.let { selected ->
            if (selected.level.size <= difficulty)
                return ""
            val chart = selected.charts[difficulty]
            return buildString {
                appendLine()
                appendLine("${selected.id}. ${selected.title}")
                appendLine("等级: ${selected.level[difficulty]} (${
                    if (selected.level[difficulty].endsWith("+") &&
                        selected.ds[difficulty] == selected.ds[difficulty].toIntFloor() + 0.5)
                        "定数未知"
                    else
                        selected.ds[difficulty]
                })")
                ChartNotes.fromList(chart.notes) ?.let { notes ->
                    appendLine("TAP: ${notes.tap}")
                    appendLine("HOLD: ${notes.hold}")
                    appendLine("SLIDE: ${notes.slide}")
                    notes.touch ?.let { touch ->
                        appendLine("TOUCH: $touch")
                    }
                    appendLine("BREAK: ${notes.breaks}")
                    if (chart.charter != "-")
                        appendLine("谱师：${chart.charter}")
                    stats[id] ?.let { stat ->
                        appendLine("查分器拟合定数：${stat[difficulty].fitDiff!!.roundDecimalPlaces(1)}")
                        stat[difficulty].avg ?.let { avg ->
                            appendLine("平均达成率：${avg.roundDecimalPlaces(2)}%")
                        }
                    }
                }
            }
        } ?: return ""
    }

    fun findByName(name: String): List<MusicInfo> = musics.values.filter {
        name.lowercase() in it.title.lowercase()
    }.take(16)
    fun findByRegex(regex: Regex): List<MusicInfo> = musics.values.filter {
        regex.find(it.title.lowercase()) != null
    }.take(16)
    fun findByCharter(charter: String): List<MusicInfo> = musics.values.filter {
        (charter.lowercase() in it.charts[3].charter) || (it.charts.size == 5 && charter.lowercase() in it.charts[4].charter)
    }

    fun filter(block: (MusicInfo) -> Boolean) = musics.values.filter(block)
    fun any(block: (MusicInfo) -> Boolean) = musics.values.any(block)

    fun <R> map(block: (MusicInfo) -> R) = musics.values.map(block)

    fun updateHot() {
        hotList = stats.map { (id, stat) -> Pair(id, stat.sumOf { it.cnt ?: .0 }) }
            .sortedByDescending { it.second }.take(400).map { it.first }
    }

    fun getStats(id: String) = stats[id]

    fun getSongWithDSByLevel(level: String): Map<Double, List<Pair<MusicInfo, Int>>> {
        val pre = map {
            it.level.mapIndexed { index, s -> if (s == level) Pair(it, index) else null }.filterNotNull()
        }.flatten()
        return pre.map { it.first.ds[it.second] }.distinct().sortedDescending().associateWith { d ->
            pre.filter { m -> m.first.ds[m.second] == d }
        }
    }

    fun findByDS(range: ClosedFloatingPointRange<Double>): String {
        val result = buildString {
            musics.values.filter { music ->
                music.ds.any {
                    it in range
                }
            }.take(50).forEach { selected ->
                selected.ds.mapIndexedNotNull { index, d -> if (d in range) index else null }.forEach { difficulty ->
                    append("${selected.id}. ${selected.title} ")
                    append(difficulty2Name(difficulty))
                    append(" ${selected.level[difficulty]} (${selected.ds[difficulty]})")
                    appendLine()
                }
            }
        }
        return result.ifBlank {
            "没有找到歌曲。\n使用方法：\n\t/定数查歌 定数\n\t/定数查歌 下限 上限"
        }
    }
    fun getPlateRemains(
        version: String,
        type: String,
        vList: List<String>,
        data: List<BriefScore>
    ): MutableList<MutableList<Pair<String, Int>>> {
        val remains = MutableList<MutableList<Pair<String, Int>>>(5) { mutableListOf() }
        data.filter {
            when (type) {
                "将" -> it.achievements < 100.0
                "极" -> it.fc.isEmpty()
                "舞舞" -> it.fs !in listOf("fsd", "fsdp")
                "神" -> it.fc !in listOf("ap", "app")
                "霸者" -> it.achievements < 80.0
                else -> false
            }
        }.forEach {
            remains[it.levelIndex].add(Pair(it.id.toString(), it.levelIndex))
        }
        filter { it.basicInfo.from in vList }.forEach { song ->
            for (i in 0 until song.ds.size) {
                if (data.none { it.id.toString() == song.id && it.levelIndex == i }) {
                    remains[i].add(Pair(song.id, i))
                }
            }
        }
        if (version != "舞" && type != "霸者")
            remains[4] = mutableListOf()
        if (version == "真")
            remains.forEach { l ->
                l.removeIf { it.first == "56" }
            }
        remains.forEach { l ->
            l.removeIf { it.first in plateExcluded }
        }
        remains[4].removeIf { it.first in remasterExcluded }
        return remains
    }
    fun plateProgress(version: String, type: String, vList: List<String>, data: List<BriefScore>): String {
        val remains = getPlateRemains(version, type, vList, data)
        return buildString {
            when {
                remains.all { it.isEmpty() } -> {
                    return "您已经达成了${version}${type}的获得条件。"
                }
                remains[3].isEmpty() && remains[4].isEmpty() -> appendLine("恭喜您已经${version}${type}确认。")
            }
            appendLine("您的${version}${type}剩余进度如下：")
            listOf("绿谱", "黄谱", "红谱", "紫谱", "白谱").forEachIndexed { i, name ->
                if (remains[i].isNotEmpty()) {
                    appendLine("${name}剩余${remains[i].size}个")
                }
            }
            val hard = (remains[3] + remains[4]).filter {
                musics[it.first]!!.ds[it.second] >= 14.6
            }.sortedByDescending { musics[it.first]!!.ds[it.second] }.take(5)
            if (hard.isNotEmpty()) {
                appendLine("高难度谱面：")
                hard.forEach {
                    val info = musics[it.first]!!
                    appendLine("${info.id}. ${info.title} ${difficulty2Name(it.second)} Lv. ${info.level[it.second]}" +
                            "(${data.find { d -> d.id.toString() == it.first && d.levelIndex == it.second }
                                ?.achievements?.roundDecimalPlaces(4)?:"0.0000"}%)")
                }
            }
            val pc = (remains.sumOf { it.size } * 1.0 / 3).toIntCeil()
            append("共计${remains.sumOf { it.size }}个，单刷需${pc}pc，即")
            if (pc / 6 != 0) // pc * 10 / 60 floor
                append("${(pc * 10.0 / 60).toIntFloor()}小时")
            if (pc * 10 % 60 != 0)
                append("${pc * 10 % 60}分钟")
        }
    }
    fun dsProgress(level: String, type: String, data: List<BriefScore>): String {
        val remains = data.filter { it.level == level }.filter {
            when (type) {
                "fc" -> it.fc.isEmpty()
                "fc+" -> it.fc !in listOf("fcp", "ap", "app")
                "ap" -> it.fc !in listOf("ap", "app")
                "ap+" -> it.fc != "app"
                "fs" -> it.fs.isEmpty()
                "fs+" -> it.fs !in listOf("fsp", "fsd", "fsdp")
                "fdx" -> it.fs !in listOf("fsd", "fsdp")
                "fdx+" -> it.fs != "fsdp"
                "clear" -> it.achievements < 80.0
                "s" -> it.achievements < 97.0
                "s+" -> it.achievements < 98.0
                "ss" -> it.achievements < 99.0
                "ss+" -> it.achievements < 99.5
                "sss" -> it.achievements < 100.0
                "sss+" -> it.achievements < 100.5
                else -> false
            }
        }.map { Pair(it.id, it.levelIndex) }.toMutableList()
        var tot = 0
        musics.values.filter { level in it.level }.forEach { song ->
            song.level.forEachIndexed { i, v ->
                if (v == level) {
                    tot += 1
                    if (data.none { it.id.toString() == song.id && it.levelIndex == i })
                        remains.add(Pair(song.id.toInt(), i))
                }
            }
        }
        if (remains.isEmpty()) {
            return "您已经达成了${level}全${type}。"
        }
        return "您的${level}${type}进度如下：\n共${tot}个谱面，已完成${tot-remains.size}个，剩余${remains.size}个"
    }
    fun getRandom(level: String, difficulty: Int = -1) =
        musics.values.filter {
            (level in it.level && it.level.size > difficulty && difficulty != -1 && it.level[difficulty] == level)
                    || (level == "" && it.level.size > difficulty)
                    || (difficulty == -1 && level in it.level)}.randomOrNull()
    fun getRandom() = musics.values.random()

    fun getRandomForRatingUp(
        target: Int = 1,
        amount: Int = 1,
        acc: Double = 100.5,
        data: PlayerData
    ): String {
        val b50 = (data.charts["sd"]!! + data.charts["dx"]!!).toMutableList()
        val b35Floor = data.charts["sd"]!!.minByOrNull { it.ra }!!.run { getNewRa(ds, achievements) }
        val b15Floor = data.charts["dx"]!!.minByOrNull { it.ra }!!.run { getNewRa(ds, achievements) }
        val selected = musics.values.map { m ->
            List(m.ds.size) { index ->
                Triple(m, index, calcB50Change(b50, m, index, acc, b35Floor, b15Floor))
            }.filter { it.third > 0 && it.third > target }
        }.flatten().shuffled().take(amount)
        if (selected.size == 1) {
            val selectedInfo = selected.first()
            val selectedSong = selectedInfo.first
            val difficulty = selectedInfo.second
            val up = selectedInfo.third
            return getInfo(selectedSong.id) + ("\n此曲${difficulty2Name(difficulty)}难度推至 100.5% 可加${
                if (target == 1) " $up " else "至少 $target "
            }分")
        } else if (selected.size > 1) {
            return buildString {
                selected.forEach {
                    appendLine("${it.first.id}. ${it.first.title} (${difficulty2Name(it.second)})")
                }
            }
        } else {
            return "未找到符合标准的推分歌曲。"
        }
    }
    fun getRandomHot(): MusicInfo {
        if (randomHotMusics.isEmpty()) {
            randomHotMusics = hotList.mapNotNull {
                musics.getOrDefault(it, null)
            }.shuffled().toMutableList()
        }
        val target = randomHotMusics.first()
        randomHotMusics.removeFirst()
        return target
    }
    fun getRandomHot(num: Int): List<MusicInfo> {
        return musics.values.shuffled().take(num)
    }
}