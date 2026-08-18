package xyz.xszq.bot.chunithm.payload

import kotlinx.serialization.Serializable

@Serializable
data class LXNSSong(
    val id: Int,
    val title: String,
    val artist: String,
    val genre: String,
    val bpm: Int,
    val map: String ?= null,
    val version: Int,
    val rights: String ?= null,
    val locked: Boolean = false,
    val disabled: Boolean = false,
    val difficulties: List<LXNSSongDifficulty>
)