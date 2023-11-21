package xyz.xszq.nereides.payload.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostGroupFile(
    @SerialName("file_type")
    val fileType: Int,
    val url: String,
    @SerialName("srv_send_msg")
    val srvSendMsg: Boolean
)
