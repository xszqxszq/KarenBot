package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSCollectionRequiredSong(
    val id: Int,
    val title: String,
    val type: String,
    val completed: Boolean ?= null,
    @SerialName("completed_difficulties")
    val completedDifficulties: List<Int> ?= null
)