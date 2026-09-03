package xyz.xszq.bot.random.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RandomOtomads(
    @SerialName("random_sites")
    val randomSites: List<String>
)