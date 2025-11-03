package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LocalCourseMusicInfo(
    val id: Int,
    val name: String = "",
    val difficulty: Int,
)
