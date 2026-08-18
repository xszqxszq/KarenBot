package xyz.xszq.bot.payload.markdown

import kotlinx.serialization.Serializable


@Serializable
data class InlineKeyboardRow(
    val buttons: List<Button>
)