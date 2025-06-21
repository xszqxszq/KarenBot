package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable

@Serializable
data class InlineKeyboard(
    val rows: List<InlineKeyboardRow>
)