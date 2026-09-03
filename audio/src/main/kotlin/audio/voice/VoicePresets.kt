package xyz.xszq.bot.audio.voice

import kotlinx.serialization.Serializable

/**
 * 活字印刷预设
 */
@Serializable
data class VoicePresets(
    /**
     * 中文预设
     */
    val presets: Map<String, List<String>>,
    /**
     * 英文预设
     */
    val englishPresets: Map<String, List<String>>
)