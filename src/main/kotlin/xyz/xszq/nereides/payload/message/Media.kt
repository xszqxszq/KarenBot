package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Media(
    @SerialName("file_uuid")
    val fileUUID: String? = null,
    @SerialName("file_info")
    val fileInfo: String,
    val ttl: Int? = null
)
