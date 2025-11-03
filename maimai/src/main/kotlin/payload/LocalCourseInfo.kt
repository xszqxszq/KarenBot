package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class LocalCourseInfo(
    val id: Int,
    val name: String,
    val mode: Int,
    val random: Boolean,
    val lower: Double,
    val upper: Double,
    val musics: List<LocalCourseMusicInfo>,
    val life: Int,
    val recover: Int,
    val damage: LocalCourseDamage
)
