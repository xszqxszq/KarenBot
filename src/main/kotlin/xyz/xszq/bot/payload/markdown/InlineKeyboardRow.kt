package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable


/**
 * Markdown 内联键盘行
 */
@Serializable
data class InlineKeyboardRow(
    val buttons: List<Button>
)