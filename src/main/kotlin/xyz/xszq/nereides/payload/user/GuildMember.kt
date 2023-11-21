package xyz.xszq.nereides.payload.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuildMember(
    @SerialName("joined_at")
    val joinedAt: String,
    val nick: String,
    val roles: List<String>
)
