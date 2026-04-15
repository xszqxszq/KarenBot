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
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.exception.AuthorizationException
import xyz.xszq.bot.chunithm.exception.UnknownException
import xyz.xszq.bot.chunithm.exception.UserNotFoundException
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.chunithm.music.Level.levelClean
import xyz.xszq.bot.chunithm.payload.*

class LXNS(
    val token: String,
    val oauthId: String,
    val oauthSecret: String,
    val oauthCallback: String,
    val chunithmData: ChunithmData
) : ChunithmAPI {
    override val id = "lxns"
    override val name = "落雪"

    private val apiServer = "https://maimai.lxns.net/api/v0/chunithm"
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

    suspend fun getMusicList(): Map<Int, MusicInfo> {
        val data = client.get("$apiServer/song/list?notes=true").body<LXNSSongs>()
        val newest = data.versions.maxByOrNull { it.version } ?: return emptyMap()
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
                isNew = song.version == newest.version,
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
                nickname = player.name,
                rating = player.rating,
                level = player.level
            ),
            settings = PlayerSettings(
                trophy = player.trophy?.id,
                plate = player.namePlate?.id,
                avatar = player.character?.id
            ),
            oldRatingList = data.bests.mapNotNull { score ->
                score.toRecord()
            },
            newRatingList = data.newBests.mapNotNull { score ->
                score.toRecord()
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
            rank = rank.orEmpty(),
            rating = Rating.calc(chart, score)
        )
    }
}