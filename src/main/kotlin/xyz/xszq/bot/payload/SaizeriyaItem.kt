package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class SaizeriyaItem(
    val id: Int,
    val name: String,
    val price: Int,
    val category: String,
    val image: Map<String, String>? = null
)
