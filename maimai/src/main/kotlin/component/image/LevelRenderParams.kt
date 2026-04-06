package xyz.xszq.bot.component.image

import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.Record

data class LevelRenderParams(
    // 模板参数
    val title: String,
    val filter: FilterParams,
    // 谱面信息
    val groups: List<Pair<String, List<ChartInfo>>>,
    // 成绩
    val matched: Map<ChartInfo, Record?>,
    val completed: Map<ChartInfo, Record?>,
)
