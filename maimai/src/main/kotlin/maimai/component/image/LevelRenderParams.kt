package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.Record

data class LevelRenderParams(
    // 模板参数
    val title: String,
    val filter: FilterParams,
    // 谱面信息
    val groups: List<Pair<String, List<ChartInfo>>>,
    // 成绩
    val matched: Map<ChartInfo, Record?>,
    val completed: Map<ChartInfo, Record?>,
    val showProgress: Boolean = false,
    val progressData: Map<MusicDifficulty, Pair<Int, Int>> = emptyMap(),
)
