package xyz.xszq.nereides.payload.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageArkObj(
    @SerialName("obj_kv")
    val objKV: List<MessageArkKv>
)
