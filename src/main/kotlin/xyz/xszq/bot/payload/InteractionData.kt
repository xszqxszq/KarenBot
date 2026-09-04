package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * 互动事件回调数据
 */
@Serializable
data class InteractionData(
    val resolved: InteractionResolved,
    val type: Int = 0
)