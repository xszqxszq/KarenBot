package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪查分器的单难度谱面信息
 *
 * @property levelValue 谱面定数
 * @property noteDesigner 谱师名
 * @property originId 原曲 ID（仅 WORLD'S END）
 * @property kanji 谱面汉字（仅 WORLD'S END）
 * @property star 谱面星级
 */
@Serializable
data class LXNSSongDifficulty(
    val difficulty: Int,
    val level: String,
    @SerialName("level_value")
    val levelValue: Double,
    @SerialName("note_designer")
    val noteDesigner: String,
    val version: Int,
    val notes: LXNSNotes ?= null,
    @SerialName("origin_id")
    val originId: Int ?= null,
    val kanji: String ?= null,
    val star: Int ?= null
)