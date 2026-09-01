package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSUserInfo(
    val sub: String ?= null,
    val name: String ?= null,
    @SerialName("preferred_username")
    val preferredUsername: String ?= null
)