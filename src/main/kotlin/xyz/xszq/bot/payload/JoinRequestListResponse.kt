package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 入群申请列表响应
 */
@Serializable
data class JoinRequestListResponse(
    val list: List<JoinRequest> = listOf(),
    @SerialName("next_cursor")
    val nextCursor: String = ""
)