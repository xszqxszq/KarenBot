package xyz.xszq.bot.maimai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xyz.xszq.bot.maimai.api.LXNS
import xyz.xszq.bot.maimai.api.MaimaiAPI
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.exception.UserBindRequiredException
import xyz.xszq.bot.maimai.music.MusicInfo
import xyz.xszq.bot.maimai.music.RatingResponse
import xyz.xszq.bot.maimai.music.Record
import xyz.xszq.bot.maimai.music.RecordsResponse
import xyz.xszq.bot.maimai.music.UserQueryParams

/**
 * 落雪查分器 mock
 *
 * @property data 曲目数据
 * @property friendCode 有效的好友码
 * @property musicId 成绩返回的曲目 ID
 */
class MockLxnsProber(
    val data: MaimaiData,
    val friendCode: Long = DEFAULT_FRIEND_CODE,
    val musicId: Int = DEFAULT_MUSIC_ID
) {
    private companion object {
        const val DEFAULT_FRIEND_CODE = 722520985289030L
        const val DEFAULT_MUSIC_ID = 852
        const val INVALID_FRIEND_CODE = "invalid friend code"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")
    }

    /**
     * 玩家信息接口认可的好友码
     */
    var playerCodes: Set<Long> = setOf(friendCode)

    /**
     * Best 50 接口认可的好友码
     */
    var ratingCodes: Set<Long> = setOf(friendCode)

    /**
     * 单曲成绩接口认可的好友码
     */
    var songCodes: Set<Long> = setOf(friendCode)

    /**
     * OAuth 拉到的好友码，为空时换票失败
     */
    var oauthFriendCode: Long ?= friendCode

    /**
     * QQ 拉到的好友码，为空时 QQ 查询失败
     */
    var qqFriendCode: Long ?= null

    /**
     * 恢复默认响应
     */
    fun reset() {
        playerCodes = setOf(friendCode)
        ratingCodes = setOf(friendCode)
        songCodes = setOf(friendCode)
        oauthFriendCode = friendCode
        qqFriendCode = null
    }

    /**
     * 建立使用 mock 响应的落雪后端
     *
     * @return 落雪后端
     */
    fun backend(): LXNS = LXNS(
        token = "test-token",
        oauthId = "test-oauth-id",
        oauthSecret = "test-oauth-secret",
        oauthCallback = "http://localhost/bind",
        maimaiData = data,
        client = client()
    )

    private fun client() = HttpClient(MockEngine { request ->
        val path = request.url.encodedPath
        val parts = path.split("/")
        val code = parts.let {
            if (it.last() == "bests") it[it.size - 2] else it.last()
        }.toLongOrNull()
        when {
            path.endsWith("/oauth/token") -> token()
            path.endsWith("/user/maimai/player/scores") -> success(scoreArray())
            path.endsWith("/user/maimai/player") -> oauthPlayer()
            path.contains("/player/qq/") -> qqPlayer()
            path.endsWith("/bests") && request.url.parameters["song_id"] != null ->
                if (code != null && code in songCodes)
                    success(scoreArray())
                else
                    failure(400, INVALID_FRIEND_CODE)
            path.endsWith("/bests") ->
                if (code != null && code in ratingCodes)
                    success(rating())
                else
                    failure(400, INVALID_FRIEND_CODE)
            code != null && code in playerCodes -> success(player(code))
            else -> failure(400, INVALID_FRIEND_CODE)
        }
    }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun MockRequestHandleScope.token(): HttpResponseData {
        if (oauthFriendCode == null)
            return failure(400, "invalid_grant")
        return respond(
            content = buildJsonObject {
                put("access_token", "test-access-token")
                put("token_type", "Bearer")
                put("expires_in", 300)
                put("refresh_token", "test-refresh-new")
                put("scope", "read")
            }.toString(),
            status = HttpStatusCode.OK,
            headers = JSON_HEADERS
        )
    }

    private fun MockRequestHandleScope.oauthPlayer(): HttpResponseData {
        val code = oauthFriendCode
        if (code == null)
            return failure(401, "unauthorized")
        return success(player(code))
    }

    private fun MockRequestHandleScope.qqPlayer(): HttpResponseData {
        val code = qqFriendCode
        if (code == null)
            return failure(404, "player not found")
        return success(player(code))
    }

    private fun MockRequestHandleScope.success(data: JsonElement) = respond(
        content = buildJsonObject {
            put("success", true)
            put("code", 200)
            put("data", data)
        }.toString(),
        status = HttpStatusCode.OK,
        headers = JSON_HEADERS
    )

    private fun MockRequestHandleScope.failure(code: Int, message: String) = respond(
        content = buildJsonObject {
            put("success", false)
            put("code", code)
            put("message", message)
        }.toString(),
        status = when (code) {
            401 -> HttpStatusCode.Unauthorized
            404 -> HttpStatusCode.NotFound
            else -> HttpStatusCode.BadRequest
        },
        headers = JSON_HEADERS
    )

    private fun player(friendCode: Long) = buildJsonObject {
        put("name", "测试玩家")
        put("rating", 13000)
        put("friend_code", friendCode)
        put("course_rank", 0)
        put("class_rank", 0)
        put("star", 87)
    }

    private fun score() = buildJsonObject {
        put("id", musicId)
        put("level_index", 3)
        put("achievements", 100.5)
        put("dx_score", 3366)
        put("type", "standard")
    }

    private fun scoreArray() = buildJsonArray {
        add(score())
    }

    private fun rating() = buildJsonObject {
        put("standard", scoreArray())
        put("dx", buildJsonArray { })
    }
}

/**
 * 建立未绑定的水鱼查分器 mock
 *
 * @return 全部查询都提示需要绑定的水鱼后端
 */
fun mockUnboundDivingFish(): MaimaiAPI = object : MaimaiAPI {
    override val id: String = "diving-fish"
    override val name: String = "水鱼"

    override suspend fun load() {}

    override suspend fun getPlayerRating(user: UserQueryParams): RatingResponse =
        throw UserBindRequiredException()

    override suspend fun getPlayerRecord(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record> = throw UserBindRequiredException()

    override suspend fun getPlayerRecords(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse = throw UserBindRequiredException()
}
