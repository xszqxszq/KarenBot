package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 入群申请
 */
@Serializable
data class JoinRequest(
    @SerialName("join_request_id")
    val id: String = "",
    @SerialName("risk_tips")
    val riskTips: String = "",
    @SerialName("union_openid")
    val unionOpenId: String = "",
    @SerialName("member_openid")
    val member: String = "",
    val username: String = "",
    @SerialName("apply_at")
    val applyAt: String = "",
    @SerialName("apply_source")
    val applySource: String = "",
    @SerialName("invited_by")
    val invitedBy: String = "",
    val bot: Boolean = false,
    @SerialName("verify_info")
    val verifyInfo: VerifyInfo ?= null
)