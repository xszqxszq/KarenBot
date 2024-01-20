package xyz.xszq.nereides.payload.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostGroupMessageResponse(
    val id: String? = null,
    val timestamp: String? = null,
    val code: Int? = null,
    val message: String? = null,
    val traceId: String? = null
)
