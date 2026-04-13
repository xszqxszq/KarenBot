package xyz.xszq.bot.chunithm.api

import xyz.xszq.bot.chunithm.music.RatingResponse
import xyz.xszq.bot.chunithm.music.UserQueryParams

interface ChunithmAPI {
    val id: String
    val name: String
    suspend fun load()
    suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse?
}