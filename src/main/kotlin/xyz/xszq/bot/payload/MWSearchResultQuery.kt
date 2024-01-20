package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class MWSearchResultQuery(val searchinfo: MWSearchResultInfo, val search: List<MWSearchResultItem>)