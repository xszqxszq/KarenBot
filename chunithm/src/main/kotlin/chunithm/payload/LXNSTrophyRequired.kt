package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 落雪查分器的称号条件
 *
 * @property difficulties 难度列表
 * @property rank 要求的评级
 * @property fullCombo 要求的全连状态
 * @property fullChain 要求的 FULL CHAIN 状态
 * @property songs 歌曲列表
 */
@Serializable
data class LXNSTrophyRequired(
    val difficulties: List<Int> ?= null,
    val rank: String ?= null,
    @SerialName("full_combo")
    val fullCombo: String ?= null,
    @SerialName("full_chain")
    val fullChain: String ?= null,
    val songs: List<LXNSTrophyRequiredSong> ?= null
)