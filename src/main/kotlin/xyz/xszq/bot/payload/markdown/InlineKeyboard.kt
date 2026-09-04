package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable

/**
 * Markdown 内联键盘
 */
@Serializable
data class InlineKeyboard(
    val rows: List<InlineKeyboardRow>
)