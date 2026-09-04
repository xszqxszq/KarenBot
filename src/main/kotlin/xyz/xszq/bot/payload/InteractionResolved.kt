package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 互动回调的按钮数据
 */
@Serializable
data class InteractionResolved(
    @SerialName("button_data")
    val buttonData: String ?= null,
    @SerialName("button_id")
    val buttonId: String ?= null
)