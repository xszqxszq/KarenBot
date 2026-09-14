package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 周期禁言规则
 */
@Serializable
data class MuteRecurringRule(
    @SerialName("task_id")
    val id: String = "",
    val weekdays: List<Int> = listOf(),
    @SerialName("start_time")
    val startTime: String = "",
    @SerialName("end_time")
    val endTime: String = "",
    val enabled: Boolean = false
)