package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪查分器的玩家信息
 *
 * @property friendCode 好友码
 * @property classEmblem 玩家的 Class
 * @property trophy 玩家当前的称号
 * @property character 玩家当前的角色
 * @property namePlate 玩家当前的名牌
 * @property mapIcon 玩家当前的地图图标
 */
@Serializable
data class LXNSPlayer(
    val name: String,
    val level: Int,
    val rating: Double,
    @SerialName("rating_possession")
    val ratingPossession: String,
    @SerialName("friend_code")
    val friendCode: Long,
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