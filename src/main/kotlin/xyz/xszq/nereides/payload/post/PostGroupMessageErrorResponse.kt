package xyz.xszq.nereides.payload.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostGroupMessageErrorResponse(
    val message: String,
    val code: Int,
    @SerialName("trace_id")
    val traceId: String? = null
)
