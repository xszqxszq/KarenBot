package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSPlayer(
    val name: String,
    val level: Int,
    val rating: Double,
    @SerialName("rating_possession")
    val ratingPossession: String,
    @SerialName("friend_code")
    val friendCode: Int,
    @SerialName("class_emblem")
    val classEmblem: LXNSClassEmblem,
    @SerialName("reborn_count")
    val rebornCount: Int,
    @SerialName("over_power")
    val overpower: Double,
    @SerialName("over_power_progress")
    val overpowerProgress: Double,
    val currency: Int,
    @SerialName("total_currency")
    val totalCurrency: Int,
    @SerialName("total_play_count")
    val totalPlayCount: Int,
    val trophy: LXNSCollection ?= null,
    val character: LXNSCollection ?= null,
    @SerialName("name_plate")
    val namePlate: LXNSCollection ?= null,
    @SerialName("map_icon")
    val mapIcon: LXNSCollection ?= null,
    @SerialName("upload_time")
    val uploadTime: String ?= null
)