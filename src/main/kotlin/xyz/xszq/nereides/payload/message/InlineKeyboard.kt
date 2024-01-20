package xyz.xszq.nereides.payload.message

import kotlinx.serialization.Serializable

@Serializable
data class InlineKeyboard(
    val rows: List<InlineKeyboardRow>
)
