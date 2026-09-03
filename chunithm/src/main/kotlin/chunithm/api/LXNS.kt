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
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Protocol
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.database.ProberBindTable
import xyz.xszq.bot.chunithm.database.QQBindTable
import xyz.xszq.bot.chunithm.exception.AuthorizationException
import xyz.xszq.bot.chunithm.exception.UnknownException
import xyz.xszq.bot.chunithm.exception.UserBindRequiredException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.music.Level.levelClean
import xyz.xszq.bot.chunithm.payload.*
import xyz.xszq.bot.toDBC
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LXNS(
    val token: String,
    val oauthId: String,
    val oauthSecret: String,
    val chunithmData: ChunithmData,
    private val client: HttpClient = createClient()
) : ChunithmAPI {
    override val id = "lxns"
    override val name = "落雪"

    private val apiServer = "https://maimai.lxns.net/api/v0/chunithm"
    private val apiOauth = "https://maimai.lxns.net/api/v0/oauth"
    private val apiUser = "https://maimai.lxns.net/api/v0/user"
    private val musics
        get() = chunithmData.musics

    private val tokenCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()

    override suspend fun load() {
    }

    suspend fun fetchSongs(): LXNSSongs = client.get("$apiServer/song/list?notes=true").body()

    suspend fun fetchTrophies(): LXNSTrophyList = client.get("$apiServer/trophy/list").body()

    suspend fun getMusicList(cached: LXNSSongs? = null): Map<Int, MusicInfo> {
        val data = cached ?: client.get("$apiServer/song/list?notes=true").body<LXNSSongs>()
        val newest = data.versions.sortedByDescending { it.version }.map { it.version }.take(2)
        val versions = data.versions.associate { version ->
            version.version to GameVersion(
                id = version.id,
                name = version.title,
                version = version.version
            )
        }
        val result = data.songs.map { song ->
            MusicInfo(
                id = song.id,
                title = song.title,
                rights = song.rights,
                artist = song.artist,
                genre = MusicGenre.of(song.genre),
                bpm = song.bpm,
                version = versions[song.version] ?: GameVersion(0, song.version.toString(), song.version),
                isNew = song.version in newest,
                locked = song.locked,
                disabled = song.disabled,
                map = song.map
            ).also { info ->
                info.charts = song.difficulties.map { chart ->
                    ChartInfo(
                        music = info,
                        difficulty = MusicDifficulty.of(chart.difficulty),
                        level = chart.level,
                        levelValue = chart.levelValue.levelClean(),
                        notes = chart.notes?.toNotes() ?: Notes(),
                        notesDesigner = chart.noteDesigner,
                        kanji = chart.kanji,
                        star = chart.star,
                        originId = chart.originId
                    )
                }
            }
        }
        result.mapNotNull { music ->
            music.charts.firstOrNull { chart -> chart.difficulty == MusicDifficulty.WorldsEnd }
        }.forEach { chart ->
            val diff = data.songs.firstOrNull { song -> song.id == chart.music.id }
                ?.difficulties
                ?.firstOrNull { it.difficulty == chart.difficulty.value }
            chart.origin = result.firstOrNull { music -> music.id == diff?.originId }
        }
        return result.associateBy { music ->
            music.id
        }
    }

    suspend fun getAliases(): Map<Int, Set<String>> = client.get("$apiServer/alias/list")
        .body<LXNSAliases>()
        .aliases
        .associate { alias ->
            alias.songId to alias.aliases.toSet()
        }

    suspend fun getTrophyList(cached: LXNSTrophyList? = null): Map<Int, LXNSTrophyInfo> {
        val data = cached ?: client.get("$apiServer/trophy/list")
            .body<LXNSTrophyList>()
        return data.trophies
            .filter { it.color == "image" && it.required != null }
            .associateBy { it.id }
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? = when (user) {
        is UserQueryParams.Username -> null
        else -> {
            println("[落雪调试] getPlayerRating user=$user")
            val friendCode = resolveFriendCode(user)
                ?: if (user is UserQueryParams.Self) throw UserBindRequiredException() else return null
            println("[落雪调试] getPlayerRating friendCode=$friendCode")
            val player = runCatching {
                getPlayerInfo(friendCode)
            }.getOrElse { e ->
                if (user is UserQueryParams.Self && e is UserNotFoundException)
                    throw UserBindRequiredException()
                throw e
            } ?: return null
            var retry = 0
            var response = client.get("$apiServer/player/$friendCode/bests") {
                setDeveloper()
            }
            while (response.status == HttpStatusCode.TooManyRequests && retry < 3) {
                retry++
                delay(retry * 2000L)
                response = client.get("$apiServer/player/$friendCode/bests") {
                    setDeveloper()
                }
            }
            val data = response.body<LXNSResponse<LXNSRatingResponse>>().data ?: run {
                println("[落雪调试] getPlayerRating bests data为null friendCode=$friendCode")
                return null
            }
            println("[落雪调试] getPlayerRating bests成功 friendCode=$friendCode")
            RatingResponse(
                player = PlayerInfo(
                    nickname = player.name.toDBC(),
                    rating = player.rating,
                    level = player.level
                ),
                settings = PlayerSettings(
                    trophy = player.trophy ?.id,
                    plate = player.namePlate ?.id,
                    avatar = player.character ?.id
                ),
                oldRatingList = data.bests.mapNotNull { score ->
                    score.toRecord()
                }.filter { record ->
                    // B50 不展示 WORLD'S END(WORLDS END 难度不计入 Rating)
                    record.chart.difficulty != MusicDifficulty.WorldsEnd
                },
                newRatingList = data.newBests.mapNotNull { score ->
                    score.toRecord()
                }.filter { record ->
                    // B50 不展示 WORLD'S END(WORLDS END 难度不计入 Rating)
                    record.chart.difficulty != MusicDifficulty.WorldsEnd
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
            println("[落雪调试] getPlayerRecord user=$user music=${music.id}")
            val friendCode = resolveFriendCode(user)
                ?: if (user is UserQueryParams.Self) throw UserBindRequiredException() else return null
            println("[落雪调试] getPlayerRecord friendCode=$friendCode")
            var retry = 0
            var response = client.get("$apiServer/player/$friendCode/bests") {
                parameter("song_id", music.id)
                setDeveloper()
            }
            while (response.status == HttpStatusCode.TooManyRequests && retry < 3) {
                retry++
                delay(retry * 2000L)
                response = client.get("$apiServer/player/$friendCode/bests") {
                    parameter("song_id", music.id)
                    setDeveloper()
                }
            }
            val body = response.body<LXNSResponse<List<LXNSScore>>>()
            println("[落雪调试] getPlayerRecord code=${body.code} friendCode=$friendCode")
            val scores = when (body.code) {
                200 -> body.data
                404, 400 -> if (user is UserQueryParams.Self) throw UserBindRequiredException() else return null
                else -> throw UnknownException(body.message)
            } ?: return null
            scores.mapNotNull { score ->
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
            if (user is UserQueryParams.Self &&
                ProberBindTable[user.event.sender.id, "lxns", "chunithm-friend-code"] == null)
                player.friendCode.toString().let { friendCode ->
                    println("[落雪调试] getPlayerRecords 补中二好友码=$friendCode sender=${user.event.sender.id}")
                    ProberBindTable[user.event.sender.id, "lxns", "chunithm-friend-code"] = friendCode
                }
            val needed = musics.map { it.id }.toSet()
            RecordsResponse(
                player = PlayerInfo(
                    nickname = player.name.toDBC(),
                    rating = player.rating,
                    level = player.level
                ),
                settings = PlayerSettings(
                    trophy = player.trophy ?.id,
                    plate = player.namePlate ?.id,
                    avatar = player.character ?.id
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
            println("[落雪调试] getPlayerRecent user=$user")
            val friendCode = resolveFriendCode(user)
                ?: if (user is UserQueryParams.Self) throw UserBindRequiredException() else return null
            println("[落雪调试] getPlayerRecent friendCode=$friendCode")
            val player = runCatching {
                getPlayerInfo(friendCode)
            }.getOrElse { e ->
                if (user is UserQueryParams.Self && e is UserNotFoundException)
                    throw UserBindRequiredException()
                throw e
            } ?: return null
            var retry = 0
            var response = client.get("$apiServer/player/$friendCode/recents") {
                setDeveloper()
            }
            while (response.status == HttpStatusCode.TooManyRequests && retry < 3) {
                retry++
                delay(retry * 2000L)
                response = client.get("$apiServer/player/$friendCode/recents") {
                    setDeveloper()
                }
            }
            val body = response.body<LXNSResponse<List<LXNSScore>>>()
            println("[落雪调试] getPlayerRecent code=${body.code} friendCode=$friendCode")
            val records = when (body.code) {
                200 -> body.data
                404, 400 -> if (user is UserQueryParams.Self) throw UserBindRequiredException() else return null
                else -> throw UnknownException(body.message)
            } ?: return null
            RecordsResponse(
                player = PlayerInfo(
                    nickname = player.name.toDBC(),
                    rating = player.rating,
                    level = player.level
                ),
                settings = PlayerSettings(
                    trophy = player.trophy ?.id,
                    plate = player.namePlate ?.id,
                    avatar = player.character ?.id
                ),
                records = records.mapNotNull { score ->
                    score.toRecord()
                }
            )
        }
    }

    private suspend fun resolveOpenid(user: UserQueryParams): String? = when (user) {
        is UserQueryParams.Self -> user.event.sender.id
        is UserQueryParams.FriendCode ->
            ProberBindTable.findIdByValue("lxns", "chunithm-friend-code", user.friendCode)
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
        val player = client.get("$apiUser/chunithm/player") { setOAuth(token) }
            .body<LXNSResponse<LXNSPlayer>>().data ?: return null
        val scores = client.get("$apiUser/chunithm/player/scores") { setOAuth(token) }
            .body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return Pair(player, scores)
    }

    suspend fun accessToken(openid: String): String? {
        println("[落雪调试] accessToken id=$openid")
        tokenCache[openid] ?.let { (token, expiresAt) ->
            if (expiresAt > System.currentTimeMillis() + 30_000L) {
                println("[落雪调试] accessToken 缓存命中 $openid")
                return token
            }
        }
        val mutex = refreshLocks.computeIfAbsent(openid) { Mutex() }
        return mutex.withLock {
            tokenCache[openid] ?.let { (token, expiresAt) ->
                if (expiresAt > System.currentTimeMillis() + 30_000L) {
                    println("[落雪调试] accessToken 缓存命中(锁内) $openid")
                    return@withLock token
                }
            }
            val refresh = ProberBindTable[openid, "lxns", "refresh"]
                ?: MaimaiSettingsTable[openid, "lxns-oa-refresh"]
                ?: run {
                    println("[落雪调试] accessToken 无refresh $openid")
                    return@withLock null
                }
            println("[落雪调试] accessToken refresh=$refresh $openid")
            val response = client.post("$apiOauth/token") {
                contentType(ContentType.Application.Json)
                setBody(LXNSOAToken(
                    clientId = oauthId,
                    clientSecret = oauthSecret,
                    grantType = "refresh_token",
                    refreshToken = refresh
                ))
            }
            val parsed = runCatching {
                response.body<LXNSOATokenResponse>()
            }.getOrNull()
            if (parsed == null) {
                if (response.status == HttpStatusCode.BadRequest || response.status == HttpStatusCode.Unauthorized) {
                    println("[落雪调试] accessToken refresh失效删除 $openid")
                    ProberBindTable.delete(openid, "lxns")
                    MaimaiSettingsTable[openid, "lxns-oa-refresh"] = ""
                }
                println("[落雪调试] accessToken 刷新失败 $openid")
                return@withLock null
            }
            println("[落雪调试] accessToken 刷新成功 $openid")
            tokenCache[openid] = Pair(
                parsed.accessToken,
                System.currentTimeMillis() + parsed.expiresIn * 1000L
            )
            MaimaiSettingsTable[openid, "lxns-oa-refresh"] = parsed.refreshToken
            ProberBindTable[openid, "lxns", "refresh"] = parsed.refreshToken
            parsed.accessToken
        }
    }

    fun HttpRequestBuilder.setDeveloper() {
        headers["Authorization"] = token
    }

    fun HttpRequestBuilder.setOAuth(
        accessToken: String
    ) {
        headers["Authorization"] = "Bearer $accessToken"
    }

    private suspend fun resolveFriendCode(user: UserQueryParams): String? = when (user) {
        is UserQueryParams.Self -> {
            ProberBindTable[user.event.sender.id, "lxns", "chunithm-friend-code"] ?: run {
                val qq = QQBindTable[user.event.sender.id] ?: return@run null
                println("[落雪调试] resolveFriendCode 无中二好友码, 用qq=$qq 拉取 sender=${user.event.sender.id}")
                val response = client.get("$apiServer/player/qq/$qq") {
                    setDeveloper()
                }.body<LXNSResponse<LXNSPlayer>>()
                when (response.code) {
                    200 -> response.data ?.let { player ->
                        println("[落雪调试] resolveFriendCode 拉到中二好友码=${player.friendCode} sender=${user.event.sender.id}")
                        ProberBindTable[user.event.sender.id, "lxns", "chunithm-friend-code"] =
                            player.friendCode.toString()
                        player.friendCode.toString()
                    }
                    else -> null
                }
            }
        }
        is UserQueryParams.FriendCode -> user.friendCode
        is UserQueryParams.Username -> null
    }

    private suspend fun getPlayerInfo(friendCode: String): LXNSPlayer? {
        var retry = 0
        while (true) {
            val response = client.get("$apiServer/player/$friendCode") {
                setDeveloper()
            }
            if (response.status == HttpStatusCode.TooManyRequests && retry < 3) {
                retry++
                delay(retry * 2000L)
                continue
            }
            val body = response.body<LXNSResponse<LXNSPlayer>>()
            return when (body.code) {
                401 -> throw AuthorizationException(body.message)
                404 -> throw UserNotFoundException(body.message)
                400 -> throw UserNotFoundException(body.message)
                200 -> body.data
                else -> throw UnknownException(body.message)
            }
        }
    }

    fun LXNSNotes.toNotes() = Notes(
        total = total,
        tap = tap,
        hold = hold,
        slide = slide,
        air = air,
        flick = flick
    )

    fun LXNSScore.toRecord(): Record? {
        val music = musics[id] ?: return null
        val chart = music.charts.getOrNull(levelIndex) ?: return null
        return Record(
            music = music,
            chart = chart,
            achievement = score,
            comboStatus = ComboStatus.of(fullCombo),
            chainStatus = ChainStatus.of(fullChain),
            clear = clear,
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
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}
