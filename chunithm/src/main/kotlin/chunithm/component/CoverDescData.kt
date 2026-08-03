package xyz.xszq.bot.chunithm.component

import kotlinx.serialization.Serializable

@Serializable
data class CoverDescData(
    val desc: String,
    val vec: List<Float>,
)
