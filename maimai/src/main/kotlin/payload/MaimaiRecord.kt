package xyz.xszq.bot.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MaimaiRecord(
    val musicId: Int,
    val level: Int,
    val playCount: Int,
    val achievement: Int,
    val comboStatus: Int,
    val syncStatus: Int,
    @SerialName("deluxscoreMax")
    val deluxeScoreMax: Int,
    val scoreRank: Int,
    val extNum1: Int,
    val extNum2: Int
)