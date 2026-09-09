package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable
import xyz.xszq.bot.maimai.music.Notes

/**
 * 本地谱面数据
 *
 * @property level 谱面等级
 * @property levelValue 谱面定数
 * @property notes 音符物量
 * @property notesDesigner 谱师名
 */
@Serializable
data class LocalChartInfo(
    val level: String,
    val levelValue: Double,
    val notes: Notes,
    val notesDesigner: String
)