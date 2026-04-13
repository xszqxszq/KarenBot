package xyz.xszq.bot.chunithm.component

import xyz.xszq.bot.Chunithm
import xyz.xszq.bot.chunithm.api.ChunithmAPI
import xyz.xszq.bot.chunithm.music.RatingResponse
import xyz.xszq.bot.chunithm.music.UserQueryParams

class ChunithmQuery(
    val chunithm: Chunithm
) {
    companion object {
        const val QUERY_FAILED = "查询失败，请重试"
    }

    fun listBackends(): List<ChunithmAPI> = listOf(
        chunithm.backend("lxns"),
        chunithm.backend("diving-fish")
    )

    suspend fun rating(
        user: UserQueryParams
    ): Pair<RatingResponse, ChunithmAPI> {
        val result = listBackends().firstNotNullOfOrNull { backend ->
            runCatching {
                backend.getPlayerRating(user)
            }.getOrNull()?.let { Pair(it, backend) }
        } ?: throw IllegalStateException(QUERY_FAILED)
        return result
    }
}