package xyz.xszq.karenbot.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteBotResponse<T>(
    val status: String,
    @SerialName("retcode")
    val retCode: Int,
    val data: T? = null,
    val message: String,
    val wording: String,
    val echo: Int
)
