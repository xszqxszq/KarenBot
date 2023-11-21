package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResumeRequest(
    val token: String,
    @SerialName("session_id")
    val sessionId: String,
    val seq: Int?
)
