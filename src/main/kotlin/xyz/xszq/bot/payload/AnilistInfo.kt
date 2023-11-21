package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class AnilistInfo(val id: Long, val idMal: Long ?= null, val title: HashMap<String, String?>,
                       val synonyms: List<String> = emptyList(), val isAdult: Boolean)
