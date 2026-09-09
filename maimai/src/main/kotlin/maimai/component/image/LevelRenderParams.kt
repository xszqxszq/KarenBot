package xyz.xszq.bot.maimai.component.image

import xyz.xszq.bot.maimai.music.ChartInfo
import xyz.xszq.bot.maimai.music.MusicDifficulty
import xyz.xszq.bot.maimai.music.Record

/**
 * 等级表渲染参数
 *
 * @property groups 组名与组内谱面
 * @property matched 谱面匹配到的成绩记录
 * @property completed 已完成的成绩记录
 * @property showProgress 是否展示完成进度
 * @property progressData 进度数据
 */
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