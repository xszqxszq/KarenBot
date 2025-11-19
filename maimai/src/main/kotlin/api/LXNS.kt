package xyz.xszq.bot.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.api.exception.*
import xyz.xszq.bot.database.MaimaiSettingsTable
import xyz.xszq.bot.database.QQBindTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.music.*
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.retry
import kotlin.math.min

class LXNS(
    val token: String,
    val oauthId: String,
    val oauthSecret: String,
    val oauthCallback: String,
    val local: Local
): MaimaiAPI {
    override val name: String = "lxns"
    val server = "https://maimai.lxns.net/api/v0/maimai"
    val oauth = "https://maimai.lxns.net/api/v0/oauth"
    val user = "https://maimai.lxns.net/api/v0/user"
    val musics
        get() = local.musics

    val json = Json {
        ignoreUnknownKeys = true
    }
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    fun HttpRequestBuilder.setDeveloper() {
        headers["Authorization"] = token
    }
    fun HttpRequestBuilder.setOAuth(accessToken: String) {
        headers["Authorization"] = "Bearer $accessToken"
    }
    suspend fun getPlayerInfo(
        event: MessageEvent,
        args: String
    ): LXNSPlayer? {
        val response = if (args.isEmpty()) {
            val qq = QQBindTable[event.sender.id] ?: throw UserBindRequiredException()
            client.get("$server/player/qq/$qq") {
                setDeveloper()
            }.body<LXNSResponse<LXNSPlayer>>()
        } else {
            client.get("$server/player/$args") {
                setDeveloper()
            }.body<LXNSResponse<LXNSPlayer>>()
        }
        when (response.code) {
            401 -> throw AuthorizationException(response.message)
            404 -> throw UserNotFoundException(response.message)
            400 -> throw UserNotFoundException(response.message)
            200 -> return response.data
            else -> throw UnknownException(response.message)
        }
    }
    override suspend fun load() {
    }

    override suspend fun getMusicList(): Map<Int, MusicInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getGameVersions(): Map<String, GameVersion> {
        TODO("Not yet implemented")
    }

    override suspend fun getPlayerRating(
        event: MessageEvent,
        args: String
    ): RatingResponse? {
        val player = getPlayerInfo(event, args) ?: return null

        val response = client.get("$server/player/${player.friendCode}/bests") {
            setDeveloper()
        }.body<LXNSResponse<LXNSRatingResponse>>().data ?: return null
        return RatingResponse(
            name = player.name,
            rating = player.rating,
            course = player.courseRank,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: player.icon?.id ?: 101,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?: player.namePlate?.id ?: 11,
            ratingList = response.standard.mapNotNull { record ->
                record.toRecord()
            },
            newRatingList = response.dx.mapNotNull { record ->
                record.toRecord()
            }
        )
    }

    private suspend fun getSingleRecord(
        player: LXNSPlayer,
        music: MusicInfo
    ): List<Record>? = retry(3) {
        val response = client.get("$server/player/${player.friendCode}/bests" +
                "?song_id=${music.resourceId}&song_type=${music.type.full}") {
            setDeveloper()
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return response.mapNotNull {
            it.toRecord()
        }
    }

    override suspend fun getPlayerRecord(
        event: MessageEvent,
        args: String,
        music: MusicInfo
    ): List<Record>? {
        val player = getPlayerInfo(event, args) ?: return null

        return getSingleRecord(player, music)
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

    override suspend fun getPlayerRecords(
        event: MessageEvent,
        args: String,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        // 玩家信息获取一定要在token获取前
        val player = getPlayerInfo(event, args) ?: return null
        val accessToken = refreshToken(event) ?: throw UserOARequiredException()

        val response = client.get("$user/maimai/player/scores") {
            setOAuth(accessToken)
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return RecordsResponse(
            name = player.name,
            rating = player.rating,
            course = player.courseRank,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: player.icon?.id ?: 101,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?: player.namePlate?.id ?: 11,
            records = response.mapNotNull { record ->
                record.toRecord()
            }
        )
    }
    fun LXNSScore.toRecord(): Record? {
        val realId = when (type) {
            "dx" -> id + 10000
            else -> id
        }
        val music = musics[realId] ?: return null
        val chart = if (music.genre == MusicGenre.Utage)
            music.charts[0]
        else
            music.charts[levelIndex]
        val achievement = (achievements * 10000).toInt()
        return Record(
            music = music,
            chart = chart,
            achievement = achievement,
            comboStatus = ComboStatus.Companion.of(fc),
            syncStatus = SyncStatus.Companion.of(fs),
            deluxeScore = dxScore,
            rate = Rate[achievement],
            rating = Rating.calc(chart, achievement)
        )
    }

    private suspend fun getRecent(
        player: LXNSPlayer
    ): List<Record>? = retry(3) {
        val response = client.get("$server/player/${player.friendCode}/recents") {
            setDeveloper()
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return response.mapNotNull {
            it.toRecord()
        }
    }
    suspend fun getPlayerRecent(
        event: MessageEvent,
        args: String
    ): RecordsResponse? {
        val player = getPlayerInfo(event, args) ?: return null
        val response = getRecent(player) ?: return null
        return RecordsResponse(
            name = player.name,
            rating = player.rating,
            course = player.courseRank,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: player.icon?.id ?: 101,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?: player.namePlate?.id ?: 11,
            records = response
        )
    }

    suspend fun initOAuth(
        code: String,
        event: MessageEvent
    ): Boolean {
        val response = client.post("$oauth/token") {
            contentType(ContentType.Application.Json)
            setBody(LXNSOAToken(
                clientId = oauthId,
                clientSecret = oauthSecret,
                grantType = "authorization_code",
                code = code,
                redirectUri = oauthCallback
            ))
        }.body<LXNSResponse<LXNSOATokenResponse>>()
        response.data ?: return false
        MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] = response.data.refreshToken
        return true
    }
    suspend fun refreshToken(
        event: MessageEvent
    ): String? {
        val refresh = MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] ?: return null
        val response = client.post("$oauth/token") {
            contentType(ContentType.Application.Json)
            setBody(LXNSOAToken(
                clientId = oauthId,
                clientSecret = oauthSecret,
                grantType = "refresh_token",
                refreshToken = refresh
            ))
        }.body<LXNSResponse<LXNSOATokenResponse>>()
        response.data ?: return null
        MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] = response.data.refreshToken
        return response.data.accessToken
    }
}