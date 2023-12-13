package xyz.xszq.nereides.payload.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostGroupMessageResponse(
    @SerialName("group_code")
    val groupCode: String,
    val msg: String,
    val ret: Int? = null
)
