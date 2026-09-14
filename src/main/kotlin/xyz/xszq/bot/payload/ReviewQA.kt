package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

/**
 * 入群申请问答
 */
@Serializable
data class ReviewQA(
    val question: String = "",
    val answer: String = ""
)