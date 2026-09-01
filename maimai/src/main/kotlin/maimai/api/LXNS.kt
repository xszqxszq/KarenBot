package xyz.xszq.bot.maimai.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Protocol
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.database.ProberBindTable
import xyz.xszq.bot.maimai.exception.*
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.min

class LXNS(
    val token: String,
    val oauthId: String,
    val oauthSecret: String,
    val oauthCallback: String,
    val maimaiData: MaimaiData,
    val client: HttpClient = createClient()
): MaimaiAPI {
    override val id: String = "lxns"
    override val name: String = "落雪"
    val apiServer = "https://maimai.lxns.net/api/v0/maimai"
    val apiOauth = "https://maimai.lxns.net/api/v0/oauth"
    val apiUser = "https://maimai.lxns.net/api/v0/user"
    val musics
        get() = maimaiData.musics

    val json = Json {
        ignoreUnknownKeys = true
    }

    private val tokenCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()

    fun HttpRequestBuilder.setDeveloper() {
        headers["Authorization"] = token
    }

    fun HttpRequestBuilder.setOAuth(accessToken: String) {
        headers["Authorization"] = "Bearer $accessToken"
    }

    suspend fun initOAuth(
        code: String,
        event: MessageEvent
    ): Boolean {
        val response = runCatching {
            client.post("$apiOauth/token") {
                contentType(ContentType.Application.Json)
                setBody(LXNSOAToken(
                    clientId = oauthId,
                    clientSecret = oauthSecret,
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = oauthCallback
                ))
            }.body<LXNSResponse<LXNSOATokenResponse>>()
        }.getOrNull() ?: return false
        val tokens = response.data ?: return false
        ProberBindTable[event.sender.id, "lxns", "refresh"] = tokens.refreshToken
        MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] = tokens.refreshToken
        runCatching {
            val info = client.get("$apiOauth/userinfo") { setOAuth(tokens.accessToken) }
                .body<LXNSResponse<LXNSUserInfo>>().data ?: return@runCatching
            info.sub ?.let { sub ->
                ProberBindTable[event.sender.id, "lxns", "id"] = sub
            }
            (info.preferredUsername ?: info.name) ?.let { username ->
                ProberBindTable[event.sender.id, "lxns", "username"] = username
            }
        }
        runCatching {
            val player = client.get("$apiUser/maimai/player") { setOAuth(tokens.accessToken) }
                .body<LXNSResponse<LXNSPlayer>>().data ?: return@runCatching
            ProberBindTable[event.sender.id, "lxns", "friend-code"] = player.friendCode.toString()
        }
        return true
    }

    fun clearTokenCache(id: String) {
        tokenCache.remove(id)
    }

    suspend fun accessToken(id: String): String? {
        tokenCache[id] ?.let { (token, expiresAt) ->
            if (expiresAt > System.currentTimeMillis() + 30_000L)
                return token
        }
        val mutex = refreshLocks.computeIfAbsent(id) { Mutex() }
        return mutex.withLock {
            tokenCache[id] ?.let { (token, expiresAt) ->
                if (expiresAt > System.currentTimeMillis() + 30_000L)
                    return@withLock token
            }
            // TODO: 不要在查分器端引入任何直接查表
            val refresh = ProberBindTable[id, "lxns", "refresh"] ?: return@withLock null
            val response = runCatching {
                client.post("$apiOauth/token") {
                    contentType(ContentType.Application.Json)
                    setBody(LXNSOAToken(
                        clientId = oauthId,
                        clientSecret = oauthSecret,
                        grantType = "refresh_token",
                        refreshToken = refresh
                    ))
                }.body<LXNSResponse<LXNSOATokenResponse>>()
            }.getOrNull() ?: return@withLock null
            val tokens = response.data ?: return@withLock null
            ProberBindTable[id, "lxns", "refresh"] = tokens.refreshToken
            MaimaiSettingsTable[id, "lxns-oa-refresh"] = tokens.refreshToken
            tokenCache[id] = Pair(
                tokens.accessToken,
                System.currentTimeMillis() + tokens.expiresIn * 1000L
            )
            tokens.accessToken
        }
    }

    suspend fun refreshToken(id: String): Boolean =
        runCatching { accessToken(id) != null }.getOrDefault(false)

    suspend fun migrateLegacyBindings(): Pair<Int, Int> {
        var ok = 0
        var skip = 0
        for (id in MaimaiSettingsTable.idsForKey("lxns-oa-refresh")) {
            if (ProberBindTable[id, "lxns", "refresh"] != null) {
                skip++
                continue
            }
            val refresh = MaimaiSettingsTable[id, "lxns-oa-refresh"] ?: continue
            ProberBindTable[id, "lxns", "refresh"] = refresh
            MaimaiSettingsTable[id, "lxns-friend-code"] ?.let { friendCode ->
                ProberBindTable[id, "lxns", "friend-code"] = friendCode
            }
            ok++
        }
        return Pair(ok, skip)
    }

    override suspend fun load() {
    }

    private suspend fun resolveOpenid(user: UserQueryParams): String? = when (user) {
        is UserQueryParams.Self -> user.event.sender.id
        is UserQueryParams.FriendCode ->
            ProberBindTable.findIdByValue("lxns", "friend-code", user.friendCode)
        is UserQueryParams.Username -> null
    }

    private fun notBoundException(user: UserQueryParams) = when (user) {
        is UserQueryParams.FriendCode ->
            UserBindRequiredException("未找到绑定该好友码的用户，请该用户先绑定查分器")
        else -> UserBindRequiredException()
    }

    private suspend fun <T> withAccessToken(
        user: UserQueryParams,
        block: suspend (String) -> T
    ): T {
        val openid = resolveOpenid(user) ?: throw notBoundException(user)
        val token = accessToken(openid) ?: throw notBoundException(user)
        return runCatching { block(token) }.getOrElse { e ->
            if (e !is AuthorizationException)
                throw e
            tokenCache.remove(openid)
            val fresh = accessToken(openid) ?: throw UserBindRequiredException()
            if (fresh == token)
                throw e
            block(fresh)
        }
    }

    private suspend fun playerAndScores(
        token: String
    ): Pair<LXNSPlayer, List<LXNSScore>>? {
        val player = client.get("$apiUser/maimai/player") { setOAuth(token) }
            .body<LXNSResponse<LXNSPlayer>>().data ?: return null
        val scores = client.get("$apiUser/maimai/player/scores") { setOAuth(token) }
            .body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return Pair(player, scores)
    }

    private suspend fun resolveFriendCode(user: UserQueryParams): String? = when (user) {
        is UserQueryParams.Self -> ProberBindTable[user.event.sender.id, "lxns", "friend-code"]
        is UserQueryParams.FriendCode -> user.friendCode
        is UserQueryParams.Username -> null
    }

    private suspend fun getPlayerInfo(friendCode: String): LXNSPlayer? {
        val response = client.get("$apiServer/player/$friendCode") {
            setDeveloper()
        }.body<LXNSResponse<LXNSPlayer>>()
        return when (response.code) {
            401 -> throw AuthorizationException(response.message)
            404 -> throw UserNotFoundException(response.message)
            400 -> throw UserNotFoundException(response.message)
            200 -> response.data
            else -> throw UnknownException(response.message)
        }
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? = when (user) {
        is UserQueryParams.Username -> null
        else -> {
            val friendCode = resolveFriendCode(user)
                ?: if (user is UserQueryParams.Self) throw UserBindRequiredException() else return null
            val player = runCatching {
                getPlayerInfo(friendCode)
            }.getOrElse { e ->
                if (user is UserQueryParams.Self && e is UserNotFoundException)
                    throw UserBindRequiredException()
                throw e
            } ?: return null
            val response = client.get("$apiServer/player/$friendCode/bests") {
                setDeveloper()
            }.body<LXNSResponse<LXNSRatingResponse>>().data ?: return null
            RatingResponse(
                player = PlayerInfo(
                    nickname = player.name,
                    rating = player.rating,
                    course = player.courseRank
                ),
                settings = PlayerSettings(
                    avatar = player.icon ?.id,
                    plate = player.namePlate ?.id
                ),
                oldRatingList = response.standard.mapNotNull { record ->
                    record.toRecord()
                },
                newRatingList = response.dx.mapNotNull { record ->
                    record.toRecord()
                }
            )
        }
    }

    override suspend fun getPlayerRecord(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record>? = when (user) {
        is UserQueryParams.Username -> null
        else -> {
            val friendCode = resolveFriendCode(user) ?: return null
            val realId = if (music.genre == MusicGenre.Utage) music.id else music.resourceId
            val realType = if (music.genre == MusicGenre.Utage) "utage" else music.type.full
            val response = client.get("$apiServer/player/$friendCode/bests") {
                parameter("song_id", realId)
                parameter("song_type", realType)
                setDeveloper()
            }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
            response.mapNotNull { score ->
                score.toRecord()
            }
        }
    }

    override suspend fun getPlayerRecords(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse? = when (user) {
        is UserQueryParams.Username -> null
        else -> withAccessToken(user) { token ->
            // 玩家信息获取一定要在token获取前
            val (player, scores) = playerAndScores(token) ?: return@withAccessToken null
            val needed = musics.map { it.id }.toSet()
            RecordsResponse(
                player = PlayerInfo(
                    nickname = player.name,
                    rating = player.rating,
                    course = player.courseRank
                ),
                settings = PlayerSettings(
                    avatar = player.icon ?.id,
                    plate = player.namePlate ?.id
                ),
                records = scores.mapNotNull { score ->
                    score.toRecord()
                }.filter { record ->
                    record.music.id in needed
                }
            )
        }
    }

    suspend fun getPlayerRecent(
        user: UserQueryParams
    ): RecordsResponse? = when (user) {
        is UserQueryParams.Username -> null
        else -> {
            val friendCode = resolveFriendCode(user) ?: return null
            val player = getPlayerInfo(friendCode) ?: return null
            val response = client.get("$apiServer/player/$friendCode/recents") {
                setDeveloper()
            }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
            RecordsResponse(
                player = PlayerInfo(
                    nickname = player.name,
                    rating = player.rating,
                    course = player.courseRank
                ),
                settings = PlayerSettings(
                    avatar = player.icon ?.id,
                    plate = player.namePlate ?.id
                ),
                records = response.mapNotNull { record ->
                    record.toRecord()
                }
            )
        }
    }

    fun LXNSScore.toRecord(): Record? {
        val realId = when {
            type == "dx" && id < 10000 -> id + 10000
            else -> id
        }
        val music = musics[realId] ?: return null
        val chart = if (music.genre == MusicGenre.Utage)
            music.charts[0]
        else
            music.charts[levelIndex]
        val achievement = (achievements * 10000).toInt()
        val rate = when {
            music.genre == MusicGenre.Utage && music.charts.size > 1 -> Rate[achievement / music.charts.size]
            else -> Rate[achievement]
        }
        val rating = when {
            music.genre == MusicGenre.Utage && music.charts.size > 1 ->
                Rating.calc(chart, achievement / music.charts.size)
            else -> Rating.calc(chart, achievement)
        }
        return Record(
            music = music,
            chart = chart,
            achievement = achievement,
            comboStatus = ComboStatus.of(fc),
            syncStatus = SyncStatus.of(fs),
            deluxeScore = dxScore,
            rate = rate,
            rating = rating
        )
    }

    fun <T> List<T>.group(size: Int = 15): List<List<T>> {
        val result = mutableListOf<List<T>>()
        var now = toMutableList()
        while (now.isNotEmpty()) {
            val num = min(size, now.size)
            val take = now.subList(0, num)
            result.add(take)
            if (num == now.size)
                break
            now = now.subList(num, now.size)
        }
        return result
    }

    companion object {
        fun Record.toLxnsScore() = LXNSScore(
            id = music.id,
            levelIndex = chart.difficulty.value,
            achievements = achievement / 10000f,
            fc = comboStatus.value,
            fs = syncStatus.value,
            dxScore = deluxeScore,
            type = music.type.value
        )

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
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}