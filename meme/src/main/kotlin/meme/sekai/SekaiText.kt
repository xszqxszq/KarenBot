package xyz.xszq.bot.meme.sekai

import kotlinx.serialization.Serializable

/**
 * PJSK 表情的文本参数
 *
 * @property x 横坐标
 * @property y 纵坐标
 * @property r 角度
 * @property s 字号
 * @property text 示例文本
 */
@Serializable
data class SekaiText(
    val text: String,
    val x: Int,
    val y: Int,
    val r: Int,
    val s: Int
)