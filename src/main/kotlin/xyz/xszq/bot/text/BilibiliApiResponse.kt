package xyz.xszq.bot.text

import kotlinx.serialization.Serializable

@Serializable
open class BilibiliApiResponse<T>(val message: String, val data: T)