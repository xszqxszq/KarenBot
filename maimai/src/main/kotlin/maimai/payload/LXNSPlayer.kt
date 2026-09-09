package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪接口的玩家资料
 *
 * @property friendCode 好友码
 * @property courseRank 段位序号
 * @property classRank 阶级序号
 * @property icon 玩家当前佩戴的头像
 * @property namePlate 玩家当前佩戴的名牌
 * @property frame 玩家当前佩戴的背景
 */
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