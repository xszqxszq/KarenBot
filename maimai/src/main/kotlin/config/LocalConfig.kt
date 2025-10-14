package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class LocalConfig(
    val server: String,
    val token: String ?= null,
    val allowed: List<String> = listOf()
)