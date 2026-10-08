package xyz.xszq.bot.feedback

import kotlinx.serialization.Serializable

/**
 * 别名举报
 */
@Serializable
internal data class ReportedAlias(
    val game: String,
    val songId: Int,
    val songName: String,
    val alias: String
)