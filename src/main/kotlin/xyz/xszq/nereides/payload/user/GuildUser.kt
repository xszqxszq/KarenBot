package xyz.xszq.nereides.payload.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuildUser(
    val id: String,
    val username: String,
    val avatar: String,
    val bot: Boolean? = null,
    @SerialName("union_openid")
    val unionOpenid: String? = null,
    @SerialName("union_user_account")
    val unionUserAccount: String? = null
)
