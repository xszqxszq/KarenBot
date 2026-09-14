package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 定时禁言规则
 */
@Serializable
data class MuteScheduleRule(
    @SerialName("task_id")
    val id: String = "",
    @SerialName("start_at")
    val startAt: String = "",
    @SerialName("end_at")
    val endAt: String = "",
    val enabled: Boolean = false
)