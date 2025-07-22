package xyz.xszq.bot

import kotlinx.serialization.Serializable

/**
 * Config for TTS.
 */
@Serializable
data class OttoConfig(
    /**
     * Presets for words that could be read directly.
     */
    val presets: Map<String, List<String>>,
    /**
     * Presets for English words that could be read directly. Higher priority.
     */
    val englishPresets: Map<String, List<String>>
)
