package xyz.xszq.bot.payload

import kotlinx.serialization.Serializable

@Serializable
data class TraceMoeResult(val anilist: AnilistInfo? = null, val filename: String? = null, val episode: String? = null,
                          val from: Double, val to: Double, val similarity: Double, val video: String = "",
                          val image: String = "")