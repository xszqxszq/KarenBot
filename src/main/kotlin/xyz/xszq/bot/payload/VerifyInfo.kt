package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 加群验证方式
 */
@Serializable
data class VerifyInfo(
    val method: String = "",
    @SerialName("verify_message")
    val verifyMessage: String = "",
    @SerialName("review_qa_list")
    val reviewQAList: List<ReviewQA> = listOf()
)