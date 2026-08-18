package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

@Serializable
data class LocalCourseMusicInfo(
    val id: Int,
    val name: String = "",
    val difficulty: Int,
)