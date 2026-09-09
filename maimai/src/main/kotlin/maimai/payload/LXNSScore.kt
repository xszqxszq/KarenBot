package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪接口中的单曲游玩成绩
 *
 * @property achievements 达成率
 * @property type 谱面类型
 * @property playTime 游玩时间点
 * @property uploadTime 上传时间点
 * @property lastPlayedTime 最近游玩时间点
 */
@Serializable
data class LXNSScore(
    val id: Int,
    @SerialName("level_index")
    val levelIndex: Int,
    val achievements: Float,
    val fc: String ?= null,
    val fs: String ?= null,
    @SerialName("dx_score")
    val dxScore: Int,
    val type: String,
    @SerialName("play_time")
    val playTime: String ?= null,
    @SerialName("upload_time")
    val uploadTime: String ?= null,
    @SerialName("last_played_time")
    val lastPlayedTime: String ?= null,
)