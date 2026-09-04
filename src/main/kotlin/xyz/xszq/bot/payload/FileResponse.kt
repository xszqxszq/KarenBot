package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 上传媒体文件的响应
 */
@Serializable
data class FileResponse(
    @SerialName("file_uuid")
    val uuid: String,
    @SerialName("file_info")
    val info: String,
    val ttl: Int,
    val id: String? = null
)