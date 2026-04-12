package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSCollectionRequired(
    val difficulties: List<Int> ?= null,
    val rate: String ?= null,
    val fc: String ?= null,
    val fs: String ?= null,
    val songs: List<LXNSCollectionRequiredSong> ?= null,
    val completed: Boolean ?= null
)
