package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

/**
 * 落雪查分器的角色列表
 *
 * 由 character/list 接口返回
 */
@Serializable
data class LXNSCharacterList(
    val characters: List<LXNSCharacterInfo>
)