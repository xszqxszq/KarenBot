package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSTrophyInfo(
    val id: Int,
    val name: String,
    val color: String ?= null,
    val description: String ?= null,
    val required: List<LXNSTrophyRequired> ?= null
)