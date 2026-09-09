package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的称号列表
 *
 * 由 trophy/list 接口返回
 */
@Serializable
data class LXNSTrophyList(
    val trophies: List<LXNSTrophyInfo>
)