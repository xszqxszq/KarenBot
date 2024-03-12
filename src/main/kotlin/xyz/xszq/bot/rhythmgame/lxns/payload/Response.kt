package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    val success: Boolean,
    val code: Int,
    val message: String,
    val data: T? = null
)
