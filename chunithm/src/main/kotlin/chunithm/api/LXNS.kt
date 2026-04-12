package xyz.xszq.bot.chunithm.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import xyz.xszq.bot.chunithm.music.ChartInfo
import xyz.xszq.bot.chunithm.music.GameVersion
import xyz.xszq.bot.chunithm.music.MusicDifficulty
import xyz.xszq.bot.chunithm.music.MusicGenre
import xyz.xszq.bot.chunithm.music.MusicInfo
import xyz.xszq.bot.chunithm.music.Notes
import xyz.xszq.bot.chunithm.payload.LXNSNotes
import xyz.xszq.bot.chunithm.payload.LXNSSongs
import xyz.xszq.bot.chunithm.record.UserQuery
import xyz.xszq.bot.chunithm.record.UserRating

/**
 * 落雪查分器
 */
class LXNS(
    val token: String,
    val oauthId: String,
    val oauthSecret: String,
    val oauthCallback: String
) : ChunithmAPI {
    override val id = "lxns"
    override val name = "落雪查分器"

    private val baseUrl = "https://maimai.lxns.net/api/v0"

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun load() {
    }

    override suspend fun getPlayerRating(query: UserQuery): UserRating {
        TODO()
    }

    /**
     * 请求头设置开发者Token
     */
    fun HttpRequestBuilder.setDeveloper() {
        headers["Authorization"] = token
    }
    /**
     * 请求头设置OAuth的AccessToken
     */
    fun HttpRequestBuilder.setOAuth(accessToken: String) {
        headers["Authorization"] = "Bearer $accessToken"
    }

    /**
     * 获取所有乐曲
     */
    suspend fun getMusicList(): Map<Int, MusicInfo> {
        val data = client.get("$baseUrl/chunithm/song/list").body<LXNSSongs>()
        val versions = data.versions.map {
            GameVersion(
                id = it.id,
                name = it.title,
                version = it.version
            )
        }
        return data.songs.map {
            MusicInfo(
                id = it.id,
                name = it.title,
                rights = it.rights,
                artist = it.artist,
                genre = MusicGenre.of(it.genre),
                bpm = it.bpm,
                version = versions.first { version -> version.version == it.version },
                locked = it.locked,
                disabled = it.disabled,
                map = it.map
            ).also { info ->
                info.charts = it.difficulties.map { chart ->
                    ChartInfo(
                        musicInfo = info,
                        difficulty = MusicDifficulty.of(chart.difficulty),
                        level = chart.level,
                        levelValue = chart.levelValue,
                        notes = chart.notes ?.toNotes() ?: Notes(),
                        notesDesigner = chart.noteDesigner,
                        kanji = chart.kanji,
                        star = chart.star
                    )
                }
            }
        }.also { musics ->
            musics.mapNotNull { it.charts.firstOrNull { chart -> chart.difficulty == MusicDifficulty.WorldsEnd } }.forEach {
                data.songs.firstOrNull { song -> song.id == it.musicInfo.id }?.difficulties
                    ?.firstOrNull { difficulty -> difficulty.difficulty == it.difficulty.id } ?.let { info ->
                        it.origin = musics.firstOrNull { music -> music.id == info.originId }
                    }
            }
        }.associateBy { it.id }
    }

    /**
     * 获取用户信息
     */
    suspend fun getPlayerInfo(query: UserQuery) {

    }

    /**
     * 转换为Notes
     */
    fun LXNSNotes.toNotes(): Notes = Notes(
        tap = tap,
        hold = hold,
        slide = slide,
        air = air,
        flick = flick
    )
}