package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSCollection(
    val id: Int,
    val name: String,
    val color: String ?= null,
    val description: String ?= null,
    val genre: String ?= null,
    val required: List<LXNSCollectionRequired> ?= null
)
