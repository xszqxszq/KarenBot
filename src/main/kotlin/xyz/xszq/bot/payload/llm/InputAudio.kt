package xyz.xszq.bot.payload.llm

import kotlinx.serialization.Serializable

/**
 * LLM 音频输入
 */
@Serializable
data class InputAudio(
    val data: String,
    val format: String
)