package xyz.xszq.bot.meme.sekai

import kotlinx.serialization.Serializable

/**
 * PJSK 角色表情数据
 *
 * @property name 角色名+编号
 * @property img 图片路径
 * @property color 文字颜色
 * @property defaultText 文本参数
 */
@Serializable
data class SekaiCharacter(
    val id: String,
    val name: String,
    val character: String,
    val img: String,
    val color: String,
    val defaultText: SekaiText
)