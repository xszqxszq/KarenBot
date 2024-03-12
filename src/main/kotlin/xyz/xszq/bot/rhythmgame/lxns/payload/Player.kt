package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val level: String,
    val rating: Int,
    @SerialName("friend_code")
    val friendCode: Int,
    @SerialName("over_power")
    val overPower: Int,
    @SerialName("change_over_power")
    val changeOverPower: Int,
    val currency: String,
    @SerialName("total_currency")
    val totalCurrency: String,
    @SerialName("total_play_count")
    val totalPlayCount: String,
    val trophy: Collection,
    val character: Collection,
    @SerialName("name_plate")
    val namePlate: Collection,
    @SerialName("map_icon")
    val mapIcon: Collection,
    @SerialName("upload_time")
    val uploadTime: String
)
