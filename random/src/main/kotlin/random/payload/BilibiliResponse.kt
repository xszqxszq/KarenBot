package xyz.xszq.bot.random.payload

import kotlinx.serialization.Serializable

@Serializable
data class BilibiliResponse<T>(
    val code: Int,
    val message: String ?= null,
    val ttl: Int,
    val data: T ?= null
)