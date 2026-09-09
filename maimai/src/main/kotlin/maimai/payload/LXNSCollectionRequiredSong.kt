package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪收藏品解锁条件中的一首曲目
 *
 * @property type 谱面类型
 * @property completed 该曲目是否已完成要求
 * @property completedDifficulties 已完成的难度列表
 */
@Serializable
data class LXNSCollectionRequiredSong(
    val id: Int,
    val title: String,
    val type: String,
    val completed: Boolean ?= null,
    @SerialName("completed_difficulties")
    val completedDifficulties: List<Int> ?= null
)