package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class UserSpecifiedPreset(
    val openId: String,
    val match: String,
    val reply: String,
)
