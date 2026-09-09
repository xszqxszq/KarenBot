package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 落雪接口中的收藏品
 *
 * 涵盖头像、名牌与背景等资源
 *
 * @property required 解锁该收藏品需要达成的条件列表
 */
@Serializable
data class LXNSCollection(
    val id: Int,
    val name: String,
    val color: String ?= null,
    val description: String ?= null,
    val genre: String ?= null,
    val required: List<LXNSCollectionRequired> ?= null
)