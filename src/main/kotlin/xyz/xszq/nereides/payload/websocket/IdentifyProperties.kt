package xyz.xszq.nereides.payload.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdentifyProperties(
    @SerialName("\$os")
    val os: String,
    @SerialName("\$browser")
    val browser: String,
    @SerialName("\$device")
    val device: String
)