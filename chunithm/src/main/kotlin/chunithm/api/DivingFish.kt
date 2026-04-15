package xyz.xszq.bot.chunithm.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.exception.UnknownException
import xyz.xszq.bot.chunithm.exception.UserDeniedException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.payload.DivingFishRatingResponse
import xyz.xszq.bot.chunithm.payload.DivingFishRecord

class DivingFish(
    val token: String,
    val chunithmData: ChunithmData
): ChunithmAPI {
    override val id: String = "diving-fish"
    override val name: String = "水鱼"

    private val server = "https://www.diving-fish.com/api/chunithmprober"
    private val musics
        get() = chunithmData.musics

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 5)
        }
    }

    override suspend fun load() {
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? {
        val response = client.post("$server/query/player") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                when (user) {
                    is UserQueryParams.QQ -> put("qq", JsonPrimitive(user.qq))
                    is UserQueryParams.Username -> put("username", JsonPrimitive(user.username))
                }
            })
        }
        when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
        }
        if (response.status != HttpStatusCode.OK)
            throw UnknownException()
        val data = response.body<DivingFishRatingResponse>()
        return RatingResponse(
            player = PlayerInfo(
                nickname = data.nickname,
                rating = data.rating
            ),
            oldRatingList = data.records.b30.mapNotNull { record ->
                record.toRecord()
            },
            newRatingList = data.records.n20.mapNotNull { record ->
                record.toRecord()
            }
        )
    }

    fun DivingFishRecord.toRecord(): Record? {
        val music = musics[mid] ?: return null
        val chart = music.charts.getOrNull(levelIndex) ?: return null
        return Record(
            music = music,
            chart = chart,
            achievement = score,
            comboStatus = ComboStatus.of(fc),
            rating = ra
        )
    }
}