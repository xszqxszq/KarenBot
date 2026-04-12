package xyz.xszq.bot.maimai.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.exception.UnknownException
import xyz.xszq.bot.maimai.exception.UserDeniedException
import xyz.xszq.bot.maimai.exception.UserNotFoundException
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.DivingFishRatingResponse
import xyz.xszq.bot.maimai.payload.DivingFishRecord
import xyz.xszq.bot.maimai.payload.DivingFishStats

class DivingFish(
    val token: String,
    val maimaiData: MaimaiData
) : MaimaiAPI {
    override val id: String = "diving-fish"
    override val name: String = "水鱼"

    val server = "https://www.diving-fish.com/api/maimaidxprober"
    val musics
        get() = maimaiData.musics

    val json = Json {
        ignoreUnknownKeys = true
    }
    val client = HttpClient(OkHttp) {
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
        val request = buildRequest(user)
        val data = ratingRequest(request) ?: return null

        return RatingResponse(
            player = PlayerInfo(
                nickname = data.nickname,
                rating = data.rating,
                course = data.additionalRating + if (data.additionalRating > 10) 1 else 0
            ),
            oldRatingList = data.charts.sd.mapNotNull { record ->
                record.toRecord()
            },
            newRatingList = data.charts.dx.mapNotNull { record ->
                record.toRecord()
            }
        )
    }

    override suspend fun getPlayerRecord(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record>? {
        val ids = listOf(music.id)
        return getRecordsDeveloper(user, ids, true) ?.records
    }

    override suspend fun getPlayerRecords(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        val ids = musics.map { it.id }
        return getRecordsDeveloper(user, ids)
    }

    suspend fun getRecordsDeveloper(
        user: UserQueryParams,
        ids: List<Int>,
        simple: Boolean = false
    ): RecordsResponse? {
        val request = buildRequest(user) {
            putJsonArray("music_id") {
                ids.forEach { id ->
                    add(JsonPrimitive(id.toString()))
                }
            }
        }
        val data = recordRequestDeveloper(request).mapNotNull { it.toRecord() }

        return getRecordsResponse(user, simple, data)
    }

    suspend fun getRecordsResponse(
        user: UserQueryParams,
        simple: Boolean = false,
        data: List<Record>
    ): RecordsResponse? = if (simple) {
        RecordsResponse(
            player = PlayerInfo(),
            records = data
        )
    } else {
        val request = buildRequest(user)
        val basicInfo = ratingRequest(request) ?: return null
        RecordsResponse(
            player = PlayerInfo(
                nickname = basicInfo.nickname,
                rating = basicInfo.rating,
                course = basicInfo.additionalRating + if (basicInfo.additionalRating > 10) 1 else 0
            ),
            records = data
        )
    }

    suspend fun ratingRequest(request: JsonObject): DivingFishRatingResponse? {
        val response = client.post("$server/query/player") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
            HttpStatusCode.OK -> response.body<DivingFishRatingResponse>()
            else -> throw UnknownException()
        }
    }

    suspend fun recordRequestDeveloper(request: JsonObject): List<DivingFishRecord> {
        val response = client.post("$server/dev/player/record") {
            contentType(ContentType.Application.Json)
            setDeveloper()
            setBody(request)
        }

        return when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
            HttpStatusCode.OK -> response.body<Map<String, List<DivingFishRecord>>>().values.flatten()
            else -> throw UnknownException()
        }
    }

    fun HttpRequestBuilder.setDeveloper() {
        headers["developer-token"] = token
    }

    fun buildRequest(
        user: UserQueryParams,
        additional: JsonObjectBuilder.() -> Unit = {}
    ): JsonObject {
        val request = buildJsonObject {
            put("b50", JsonPrimitive(true))
            when (user) {
                is UserQueryParams.QQ -> put("qq", JsonPrimitive(user.qq))
                is UserQueryParams.Username -> put("username", JsonPrimitive(user.username))
            }
            additional()
        }
        return request
    }

    fun DivingFishRecord.toRecord(): Record? {
        val music = musics[songId] ?: return null
        val chart = if (music.genre == MusicGenre.Utage)
            music.charts[0]
        else
            music.charts[levelIndex]
        val achievement = (achievements * 10000).toInt()
        return Record(
            music = music,
            chart = chart,
            achievement = achievement,
            comboStatus = ComboStatus.of(fc),
            syncStatus = SyncStatus.of(fs),
            deluxeScore = dxScore,
            rate = rate,
            rating = Rating.calc(chart, achievement)
        )
    }

    suspend fun getStats(): DivingFishStats = client.get("$server/chart_stats").body<DivingFishStats>()
}