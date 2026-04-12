package xyz.xszq.bot.maimai.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.database.MaimaiSettingsTable
import xyz.xszq.bot.maimai.exception.AuthorizationException
import xyz.xszq.bot.maimai.exception.UnknownException
import xyz.xszq.bot.maimai.exception.UserNotFoundException
import xyz.xszq.bot.maimai.exception.UserOARequiredException
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.*
import kotlin.math.min

class LXNS(
    val token: String,
    val oauthId: String,
    val oauthSecret: String,
    val oauthCallback: String,
    val maimaiData: MaimaiData
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
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 5)
        }
    }
    fun HttpRequestBuilder.setDeveloper() {
        headers["Authorization"] = token
    }
    fun HttpRequestBuilder.setOAuth(accessToken: String) {
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
    override suspend fun load() {
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? {
        val player = getPlayerInfo(user) ?: return null

        val response = client.get("$apiServer/player/${player.friendCode}/bests") {
            setDeveloper()
        }.body<LXNSResponse<LXNSRatingResponse>>().data ?: return null
        return RatingResponse(
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

    private suspend fun getSingleRecord(
        player: LXNSPlayer,
        music: MusicInfo
    ): List<Record>? {
        val realId = when {
            music.genre == MusicGenre.Utage -> music.id
            music.type == MusicType.Deluxe -> music.resourceId
            else -> music.id
        }
        val realType = when {
            music.genre == MusicGenre.Utage -> "utage"
            else -> music.type.full
        }
        val response = client.get("$apiServer/player/${player.friendCode}/bests" +
                "?song_id=$realId&song_type=$realType") {
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
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        // 玩家信息获取一定要在token获取前
        val player = getPlayerInfo(user) ?: return null
        val accessToken = refreshToken(user.event) ?: throw UserOARequiredException()

        val response = client.get("$apiUser/maimai/player/scores") {
            setOAuth(accessToken)
        }.body<LXNSResponse<List<LXNSScore>>>().data ?: return null
        return RecordsResponse(
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
                nickname = player.name,
                rating = player.rating,
                course = player.courseRank
            ),
            settings = PlayerSettings(
                avatar = player.icon ?.id,
                plate = player.namePlate ?.id
            ),
            records = response
        )
    }

    suspend fun initOAuth(
        code: String,
        event: MessageEvent
    ): Boolean {
        val response = client.post("$apiOauth/token") {
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
        // TODO: 不要在查分器端引入任何直接查表
        val refresh = MaimaiSettingsTable[event.sender.id, "lxns-oa-refresh"] ?: return null
        val response = client.post("$apiOauth/token") {
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