package xyz.xszq.bot.maimai.payload

import kotlinx.serialization.Serializable

/**
 * 别名举报
 */
@Serializable
internal data class ReportingAlias(
    val game: String,
    val songId: Int,
    val songName: String,
    val alias: String
)
