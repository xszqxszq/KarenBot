package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class TraceMoeResults(val result: List<TraceMoeResult>)