package xyz.xszq.bot.random.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 随机音MAD列表
 */
@Serializable
data class RandomOtomads(
    @SerialName("random_sites")
    val randomSites: List<String>
)