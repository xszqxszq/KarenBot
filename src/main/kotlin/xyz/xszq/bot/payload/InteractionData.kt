package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class InteractionData(
    val resolved: InteractionResolved,
    val type: Int
)
