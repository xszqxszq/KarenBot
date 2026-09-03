package xyz.xszq.bot.chunithm.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.ConnectionPool
import okhttp3.Protocol
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.chunithm.exception.*
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.payload.DivingFishOAuthTokenResponse
import xyz.xszq.bot.chunithm.payload.DivingFishRatingResponse
import xyz.xszq.bot.chunithm.payload.DivingFishRecord
import xyz.xszq.bot.chunithm.payload.DivingFishRecordsResponse
import xyz.xszq.bot.toDBC
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DivingFish(
    val oauthId: String,
    val oauthSecret: String,
    val chunithmData: ChunithmData,
    private val client: HttpClient = createClient()
): ChunithmAPI {
    override val id: String = "diving-fish"
    override val name: String = "水鱼"

    private val server = "https://www.diving-fish.com/api/chunithmprober"
    private val authServer = "https://auth.diving-fish.com"
    private val musics
        get() = chunithmData.musics

    private val tokenCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()

    val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun load() {
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? = when (user) {
        is UserQueryParams.Self -> {
            println("[水鱼调试] getPlayerRating Self sender=${user.event.sender.id}")
            val data = withUserToken(user) { token ->
                ratingRequest(buildJsonObject { }, token)
            } ?: return null
            data.toRatingResponse()
        }
        is UserQueryParams.Username -> {
            val data = ratingRequest(buildJsonObject {
                put("username", JsonPrimitive(user.username))
            }) ?: return null
            data.toRatingResponse()
        }
        is UserQueryParams.FriendCode -> null
    }

    private fun DivingFishRatingResponse.toRatingResponse(): RatingResponse = RatingResponse(
        player = PlayerInfo(
            nickname = nickname.toDBC(),
            rating = rating
        ),
        oldRatingList = records.b30.mapNotNull { record ->
            record.toRecord()
        },
        newRatingList = records.n20.mapNotNull { record ->
            record.toRecord()
        }
    )

    override suspend fun getPlayerRecord(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record>? = when (user) {
        is UserQueryParams.FriendCode -> null
        else -> {
            println("[水鱼调试] getPlayerRecord user=$user")
            withUserToken(user) { token ->
                recordsRequest(token, listOf(music.id)).records.best.mapNotNull { record ->
                    record.toRecord()
                }
            }
        }
    }

    override suspend fun getPlayerRecords(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse? = when (user) {
        is UserQueryParams.FriendCode -> null
        else -> {
            println("[水鱼调试] getPlayerRecords user=$user")
            val ids = musics.map { it.id }
            val data = withUserToken(user) { token ->
                recordsRequest(token, ids)
            }
            RecordsResponse(
                player = PlayerInfo(
                    nickname = data.nickname.toDBC(),
                    rating = data.rating
                ),
                records = data.records.best.filter { record ->
                    record.mid in ids
                }.mapNotNull { record ->
                    record.toRecord()
                }
            )
        }
    }

    private suspend fun accessTokenFor(user: UserQueryParams): String? = when (user) {
        is UserQueryParams.Self -> accessToken(user.event.sender.id)
        is UserQueryParams.Username ->
            ProberBindTable.findIdByValue("diving-fish", "username", user.username)
                ?.let { openid ->
                    accessToken(openid)
                }
        is UserQueryParams.FriendCode -> null
    }

    private suspend fun <T> withUserToken(
        user: UserQueryParams,
        block: suspend (String) -> T
    ): T {
        val token = accessTokenFor(user) ?: throw UserBindRequiredException()
        return runCatching { block(token) }.getOrElse { e ->
            if (e is UserNotFoundException)
                throw UserBindRequiredException()
            throw e
        }
    }

    suspend fun ratingRequest(
        request: JsonObject,
        token: String ?= null
    ): DivingFishRatingResponse? {
        var retry = 0
        while (true) {
            val response = client.post("$server/query/player") {
                contentType(ContentType.Application.Json)
                setBody(request)
                token ?.let { setOAuth(it) }
            }
            when (response.status) {
                HttpStatusCode.Unauthorized -> throw AuthorizationException()
                HttpStatusCode.BadRequest -> throw UserNotFoundException()
                HttpStatusCode.Forbidden -> throw UserDeniedException()
                HttpStatusCode.TooManyRequests -> {
                    if (retry >= 3)
                        throw UnknownException("已超出今日请求上限")
                    retry++
                    delay(retry * 2000L)
                }
                HttpStatusCode.OK -> return response.body<DivingFishRatingResponse>()
                else -> throw UnknownException("HTTP ${response.status.value}")
            }
        }
    }

    suspend fun recordsRequest(
        token: String,
        ids: List<Int> = emptyList()
    ): DivingFishRecordsResponse {
        var requestIds = ids
        var retry = 0
        while (true) {
            val response = client.get("$server/player/records") {
                if (requestIds.isNotEmpty() && requestIds.size < 1000)
                    parameter("song_id", requestIds.joinToString(","))
                setOAuth(token)
            }
            when (response.status) {
                HttpStatusCode.OK -> return response.body<DivingFishRecordsResponse>()
                HttpStatusCode.Unauthorized -> throw AuthorizationException()
                HttpStatusCode.BadRequest -> throw UserNotFoundException()
                HttpStatusCode.Forbidden -> throw UserDeniedException()
                HttpStatusCode.TooManyRequests -> {
                    if (retry >= 3)
                        throw UnknownException("已超出今日请求上限")
                    retry++
                    delay(retry * 2000L)
                }
                HttpStatusCode.RequestURITooLong -> {
                    if (requestIds.isEmpty() || retry >= 1)
                        throw UnknownException("HTTP 414")
                    retry++
                    requestIds = emptyList()
                }
                else -> throw UnknownException("HTTP ${response.status.value}")
            }
        }
    }

    suspend fun accessToken(openid: String): String? {
        println("[水鱼调试] accessToken openid=$openid")
        tokenCache[openid] ?.let { (token, expiresAt) ->
            if (expiresAt > System.currentTimeMillis() + 30_000L) {
                println("[水鱼调试] accessToken 缓存命中 $openid")
                return token
            }
        }
        val mutex = refreshLocks.computeIfAbsent(openid) { Mutex() }
        return mutex.withLock {
            tokenCache[openid] ?.let { (token, expiresAt) ->
                if (expiresAt > System.currentTimeMillis() + 30_000L) {
                    println("[水鱼调试] accessToken 缓存命中(锁内) $openid")
                    return@withLock token
                }
            }
            val sub = ProberBindTable[openid, "diving-fish", "id"]
                ?: run {
                    println("[水鱼调试] accessToken 无sub $openid")
                    return@withLock null
                }
            println("[水鱼调试] accessToken sub=$sub $openid")
            val tokens = onBehalfOf("sub:$sub")
            println("[水鱼调试] accessToken 换票成功 $openid expiresIn=${tokens.expiresIn}")
            tokenCache[openid] = Pair(
                tokens.accessToken,
                System.currentTimeMillis() + tokens.expiresIn * 1000L
            )
            tokens.accessToken
        }
    }

    private suspend fun onBehalfOf(subject: String): DivingFishOAuthTokenResponse {
        println("[水鱼调试] onBehalfOf subject=$subject")
        var retry = 0
        while (true) {
            val response = client.post("$authServer/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(formUrlEncode(
                    "grant_type" to "urn:diving-fish:params:oauth:grant-type:on-behalf-of",
                    "client_id" to oauthId,
                    "client_secret" to oauthSecret,
                    "subject" to subject
                ))
            }
            println("[水鱼调试] onBehalfOf subject=$subject status=${response.status}")
            if (response.status != HttpStatusCode.TooManyRequests || retry >= 3) {
                if (response.status == HttpStatusCode.BadRequest)
                    throw UserBindRequiredException()
                if (!response.status.isSuccess())
                    throw UnknownException()
                return response.body<DivingFishOAuthTokenResponse>()
            }
            retry++
            delay(retry * 2000L)
        }
    }

    private fun formUrlEncode(
        vararg pairs: Pair<String, String>
    ): String = pairs.joinToString("&") { (key, value) ->
        "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
    }

    fun HttpRequestBuilder.setOAuth(accessToken: String) {
        headers["Authorization"] = "Bearer $accessToken"
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

    companion object {
        fun createClient() = HttpClient(OkHttp) {
            engine {
                config {
                    connectionPool(ConnectionPool(
                        maxIdleConnections = 5,
                        keepAliveDuration = 5,
                        timeUnit = TimeUnit.MINUTES
                    ))
                    protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 5)
                retryOnExceptionIf { request, _ ->
                    request.method == HttpMethod.Post
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}
