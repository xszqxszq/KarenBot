package xyz.xszq.bot.otto.voice

import kotlinx.serialization.Serializable

/**
 * TTS Voice Presets.
 */
@Serializable
data class VoicePresets(
    /**
     * Presets for words that could be read directly.
     */
    val presets: Map<String, List<String>>,
    /**
     * Presets for English words that could be read directly. Higher priority.
     */
    val englishPresets: Map<String, List<String>>
)
