package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LXNSPlayer(
    val name: String,
    val rating: Int,
    @SerialName("friend_code")
    val friendCode: Long,
    @SerialName("course_rank")
    val courseRank: Int,
    @SerialName("class_rank")
    val classRank: Int,
    val star: Int,
    val icon: LXNSCollection ?= null,
    @SerialName("name_plate")
    val namePlate: LXNSCollection ?= null,
    val frame: LXNSCollection ?= null,
)