package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MWSearchResult(val query: MWSearchResultQuery)