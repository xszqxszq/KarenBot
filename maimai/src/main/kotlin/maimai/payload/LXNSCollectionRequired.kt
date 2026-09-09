package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 落雪收藏品的解锁条件
 *
 * @property difficulties 限定的曲目难度列表
 * @property rate 要求的达成率段位
 * @property fc 要求的全连状态
 * @property fs 要求的同步状态
 * @property songs 限定的具体曲目列表
 * @property completed 条件是否已达成
 */
@Serializable
data class LXNSCollectionRequired(
    val difficulties: List<Int> ?= null,
    val rate: String ?= null,
    val fc: String ?= null,
    val fs: String ?= null,
    val songs: List<LXNSCollectionRequiredSong> ?= null,
    val completed: Boolean ?= null
)