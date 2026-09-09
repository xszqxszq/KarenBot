package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的称号
 *
 * @property required 称号条件
 */
@Serializable
data class LXNSTrophyInfo(
    val id: Int,
    val name: String,
    val color: String ?= null,
    val description: String ?= null,
    val required: List<LXNSTrophyRequired> ?= null
)