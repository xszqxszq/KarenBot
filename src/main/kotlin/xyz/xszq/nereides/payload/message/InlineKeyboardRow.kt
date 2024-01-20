package xyz.xszq.nereides.payload.message

import kotlinx.serialization.Serializable

@Serializable
data class InlineKeyboardRow(
    val buttons: List<Button>
)
