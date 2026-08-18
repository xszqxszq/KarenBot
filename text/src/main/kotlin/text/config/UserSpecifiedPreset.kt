package xyz.xszq.bot.text.config

import kotlinx.serialization.Serializable

@Serializable
data class UserSpecifiedPreset(
    val openId: String,
    val match: String,
    val reply: String,
)
