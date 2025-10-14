package xyz.xszq.bot.config

import kotlinx.serialization.Serializable

@Serializable
data class ElasticsearchConfig(
    val host: String,
    val username: String,
    val password: String
)
