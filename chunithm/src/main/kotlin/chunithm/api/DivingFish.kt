package xyz.xszq.bot.chunithm.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.exception.UnknownException
import xyz.xszq.bot.chunithm.exception.UserDeniedException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.payload.DivingFishRatingResponse
import xyz.xszq.bot.chunithm.payload.DivingFishRecord
import xyz.xszq.bot.chunithm.payload.DivingFishRecordsResponse
import xyz.xszq.bot.toDBC

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
            retryOnExceptionIf { request, response ->
                request.method == HttpMethod.Post
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
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
                nickname = data.nickname.toDBC(),
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
        val data = recordsRequestDeveloper(user)

        return getRecordsResponse(simple, ids, data)
    }

    fun getRecordsResponse(
        simple: Boolean = false,
        ids: List<Int>,
        data: DivingFishRecordsResponse
    ): RecordsResponse? = if (simple) {
        RecordsResponse(
            player = PlayerInfo(),
            records = data.records.best.filter { it.mid in ids }.mapNotNull { it.toRecord() }
        )
    } else {
        RecordsResponse(
            player = PlayerInfo(
                nickname = data.nickname.toDBC(),
                rating = data.rating
            ),
            records = data.records.best.filter { it.mid in ids }.mapNotNull { it.toRecord() }
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

    suspend fun recordsRequestDeveloper(
        user: UserQueryParams
    ): DivingFishRecordsResponse {
        val response = client.get(URLBuilder("$server/dev/player/records").apply {
            when (user) {
                is UserQueryParams.QQ -> parameters["qq"] = user.qq.toString()
                is UserQueryParams.Username -> parameters["username"] = user.username
            }
        }.build()) {
            setDeveloper()
        }

        return when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
            HttpStatusCode.OK -> response.body<DivingFishRecordsResponse>()
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
            when (user) {
                is UserQueryParams.QQ -> put("qq", JsonPrimitive(user.qq))
                is UserQueryParams.Username -> put("username", JsonPrimitive(user.username))
            }
            additional()
        }
        return request
    }

    fun DivingFishRecord.toRecord(): Record? {
        val music = musics[mid] ?: return null
        val chart = music.charts.getOrNull(levelIndex) ?: return null
        return Record(
            music = music,
            chart = chart,
            achievement = score,
            comboStatus = ComboStatus.of(fc),
            rating = Rating.calc(chart, score)
        )
    }
}