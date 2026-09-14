package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 群聊禁言规则
 */
@Serializable
data class GlobalMuteRule(
    val mode: String = "none",
    @SerialName("schedule_rules")
    val scheduleRules: List<MuteScheduleRule> = listOf(),
    @SerialName("recurring_rules")
    val recurringRules: List<MuteRecurringRule> = listOf()
)