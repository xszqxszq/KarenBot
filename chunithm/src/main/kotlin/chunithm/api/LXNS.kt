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
import okhttp3.ConnectionPool
import okhttp3.Protocol
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.database.MaimaiSettingsTable
import xyz.xszq.bot.chunithm.exception.AuthorizationException
import xyz.xszq.bot.chunithm.exception.UnknownException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.exception.UserOARequiredException
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.music.Level.levelClean
import xyz.xszq.bot.chunithm.payload.*
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.toDBC
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
    ): RatingResponse? {
        val player = getPlayerInfo(user) ?: return null
        val response = client.get("$apiServer/player/${player.friendCode}/bests") {
            setDeveloper()
        }
        if (response.status != HttpStatusCode.OK)
            return null
        val data = response.body<LXNSResponse<LXNSRatingResponse>>().data ?: return null
        return RatingResponse(
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
            },
            newRatingList = data.newBests.mapNotNull { score ->
                score.toRecord()
            }
        )
    }

    private suspend fun getSingleRecord(
        player: LXNSPlayer,
        music: MusicInfo
    ): List<Record>? {
        val response = client.get("$apiServer/player/${player.friendCode}/bests" +
                "?song_id=${music.id}") {
            setDeveloper()
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return response.mapNotNull {
            it.toRecord()
        }
    }

    override suspend fun getPlayerRecord(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record>? {
        val player = getPlayerInfo(user) ?: return null

        return getSingleRecord(player, music)
    }

    override suspend fun getPlayerRecords(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        // 玩家信息获取一定要在token获取前
        val player = getPlayerInfo(user) ?: return null
        val accessToken = refreshToken(user.event) ?: throw UserOARequiredException()

        val response = client.get("$apiUser/chunithm/player/scores") {
            setOAuth(accessToken)
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return RecordsResponse(
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
            records = response.mapNotNull { record ->
                record.toRecord()
            }
        )
    }

    fun HttpRequestBuilder.setDeveloper() {
        headers["Authorization"] = token
    }

    fun HttpRequestBuilder.setOAuth(
        accessToken: String
    ) {
        headers["Authorization"] = "Bearer $accessToken"
    }

    suspend fun getPlayerInfo(
        user: UserQueryParams
    ): LXNSPlayer? {
        val response = client.get(when (user) {
            is UserQueryParams.QQ -> "$apiServer/player/qq/${user.qq}"
            is UserQueryParams.Username -> "$apiServer/player/${user.username}"
        }) {
            setDeveloper()
        }.body<LXNSResponse<LXNSPlayer>>()
        when (response.code) {
            401 -> throw AuthorizationException(response.message)
            404 -> throw UserNotFoundException(response.message)
            400 -> throw UserNotFoundException(response.message)
            200 -> return response.data
            else -> throw UnknownException(response.message)
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
    private suspend fun getRecent(
        player: LXNSPlayer
    ): List<Record>? {
        val response = client.get("$apiServer/player/${player.friendCode}/recents") {
            setDeveloper()
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return response.mapNotNull {
            it.toRecord()
        }
    }
    suspend fun getPlayerRecent(
        user: UserQueryParams
    ): RecordsResponse? {
        val player = getPlayerInfo(user) ?: return null
        val response = getRecent(player) ?: return null
        return RecordsResponse(
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
            records = response
        )
    }
    suspend fun refreshToken(
        event: MessageEvent
    ): String? {
        // TODO: 不要在查分器端引入任何直接查表
        val cached = MaimaiSettingsTable[event.sender.id, "lxns-oa-access"]
        val expires = MaimaiSettingsTable[event.sender.id, "lxns-oa-expires"]?.toLongOrNull()
        if (cached != null && expires != null && now() < expires - 300)
            return cached

        val refresh = MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] ?: return null
        val response = client.post("$apiOauth/token") {
            contentType(ContentType.Application.Json)
            setBody(LXNSOAToken(
                clientId = oauthId,
                clientSecret = oauthSecret,
                grantType = "refresh_token",
                refreshToken = refresh
            ))
        }.body<LXNSOATokenResponse>()
        MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] = response.refreshToken
        MaimaiSettingsTable[event.sender.id, "lxns-oa-access"] = response.accessToken
        MaimaiSettingsTable[event.sender.id, "lxns-oa-expires"] = (now() + response.expiresIn).toString()
        return response.accessToken
    }
    private fun now() = System.currentTimeMillis() / 1000

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