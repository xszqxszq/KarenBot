package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的牌子列表
 *
 * 由 plate/list 接口返回
 */
@Serializable
data class LXNSPlateList(
    val plates: List<LXNSPlateInfo>
)