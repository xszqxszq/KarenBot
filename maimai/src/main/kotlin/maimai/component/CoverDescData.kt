package xyz.xszq.bot.maimai.component

import kotlinx.serialization.Serializable

@Serializable
data class CoverDescData(
    val desc: String,
    val vec: List<Float>,
)
