package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MWSearchResultInfo(val totalhits: Int)