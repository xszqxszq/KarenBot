package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 本地段位考核信息
 *
 * @property id 段位 ID
 * @property name 段位名称
 * @property mode 段位模式
 * @property random 是否为随机选曲
 * @property lower 随机选曲的定数下界
 * @property upper 随机选曲的定数上界
 * @property musics 段位包含的歌曲
 * @property life 初始血量
 * @property recover 每一首后恢复的血量
 * @property damage 扣血值详情
 */
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