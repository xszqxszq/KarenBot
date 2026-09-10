package xyz.xszq.bot.maimai

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import xyz.xszq.bot.maimai.api.DivingFish
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.payload.DivingFishCharts
import xyz.xszq.bot.maimai.payload.DivingFishOAuthTokenResponse
import xyz.xszq.bot.maimai.payload.DivingFishRatingResponse
import xyz.xszq.bot.maimai.payload.DivingFishRecord
import xyz.xszq.bot.maimai.payload.DivingFishRecordsResponse
import java.io.File
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Base64
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.Charsets.UTF_8

/**
 * 模拟水鱼查分器
 *
 * @property data 曲目数据
 * @property accountIds 水鱼账号标识
 * @property refIds 外部标识
 * @property musicId 成绩返回的曲目 ID
 * @property latencyMs 接口延迟（毫秒）
 */
class MockDivingFish(
    val data: MaimaiData,
    val accountIds: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    val refIds: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    val musicId: Int = DEFAULT_MUSIC_ID,
    private val latencyMs: Long = 0
) {
    private companion object {
        const val DEFAULT_MUSIC_ID = 852
        const val DEFAULT_RATING = 13000
        const val OAUTH_ID = "test-diving-fish-oa-id"
        const val OAUTH_SECRET = "test-diving-fish-oa-secret"
        const val USERNAME = "test-diving-fish"
        const val MUSIC_CACHE = "diving-fish.json"
        const val STATS_CACHE = "diving-fish-stats.json"
        const val EMPTY_STATS = """{"charts":{},"diff_data":{}}"""
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")
        val json = Json { ignoreUnknownKeys = true }
    }

    private val oauthId = OAUTH_ID
    private val oauthSecret = OAUTH_SECRET

    /**
     * 建立水鱼后端
     *
     * @return 水鱼后端
     */
    fun backend(): DivingFish = DivingFish(
        oauthId = oauthId,
        oauthSecret = oauthSecret,
        maimaiData = data,
        client = client()
    )

    /**
     * 为指定用户预置账号标识
     *
     * @param externalId 用户 OpenID 或 QQ 号
     * @return 账号标识
     */
    fun bind(externalId: String): String = "$externalId-sub".also { sub ->
        accountIds += sub
        refIds += externalId
    }

    private fun client() = HttpClient(MockEngine { request ->
        delay(latencyMs)
        val path = request.url.encodedPath
        when {
            path.endsWith("/oauth/token") -> token(request)
            path.endsWith("/music_data") -> cached(MUSIC_CACHE, "[]")
            path.endsWith("/chart_stats") -> cached(STATS_CACHE, EMPTY_STATS)
            path.endsWith("/query/player") -> rating(request)
            path.endsWith("/player/records") -> records(request)
            else -> failure(HttpStatusCode.NotFound, "unknown endpoint")
        }
    }) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun MockRequestHandleScope.token(request: HttpRequestData): HttpResponseData {
        val form = parseForm(request.bodyText())
        if (form["client_id"] != oauthId || form["client_secret"] != oauthSecret)
            return tokenFailure(HttpStatusCode.Unauthorized, "invalid_client")
        val subject = form["subject"]
            ?: return tokenFailure(HttpStatusCode.BadRequest, "invalid_request")
        val sub = resolveSub(subject)
            ?: return tokenFailure(HttpStatusCode.BadRequest, "consent_required")
        return respondJson(
            json.encodeToString(
                DivingFishOAuthTokenResponse(
                    tokenType = "Bearer",
                    accessToken = tokenFor(sub),
                    expiresIn = 300,
                    scope = "prober.records.read"
                )
            )
        )
    }

    private fun MockRequestHandleScope.rating(request: HttpRequestData): HttpResponseData {
        val query = runCatching {
            json.parseToJsonElement(request.bodyText())
                .jsonObject["username"] ?.jsonPrimitive ?.content
        }.getOrNull()
        if (query != null) {
            if (query != USERNAME)
                return respond(
                    content = """{"message":"user not exists"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = JSON_HEADERS
                )
            return respondJson(json.encodeToString(ratingResponse()))
        }
        if (authorized(request) == null)
            return failure(HttpStatusCode.Unauthorized, "令牌无效或已过期")
        return respondJson(json.encodeToString(ratingResponse()))
    }

    private fun MockRequestHandleScope.records(request: HttpRequestData): HttpResponseData {
        if (authorized(request) == null)
            return failure(HttpStatusCode.Unauthorized, "令牌无效或已过期")
        val requested = request.url.parameters["song_id"]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
        val records = listOf(record()).filter { record ->
            requested == null || record.songId in requested
        }
        return respondJson(
            json.encodeToString(
                DivingFishRecordsResponse(
                    username = USERNAME,
                    rating = DEFAULT_RATING,
                    additionalRating = 0,
                    nickname = "测试玩家",
                    records = records
                )
            )
        )
    }

    private fun authorized(request: HttpRequestData): String? {
        val header = request.headers["Authorization"] ?: return null
        val part = header.removePrefix("Bearer ").trim().split(".").getOrNull(1) ?: return null
        val sub = runCatching {
            Base64.getUrlDecoder().decode(part).decodeToString()
        }.getOrNull() ?: return null
        val claimed = runCatching {
            json.parseToJsonElement(sub).jsonObject["sub"]?.jsonPrimitive?.content
        }.getOrNull() ?: return null
        return claimed.takeIf { it in accountIds }
    }

    private fun resolveSub(subject: String): String? = when {
        subject.startsWith("sub:") -> subject.removePrefix("sub:").takeIf { it in accountIds }
        subject.startsWith("ref:") -> {
            val digest = subject.removePrefix("ref:")
            refIds.firstOrNull { sha256Hex("$oauthId:$it") == digest } ?.let { external ->
                bind(external)
            }
        }
        else -> null
    }

    private fun tokenFor(sub: String): String = listOf(
        """{"alg":"none"}""",
        """{"sub":"$sub"}"""
    ).joinToString(".") { part ->
        Base64.getUrlEncoder().withoutPadding().encodeToString(part.toByteArray(UTF_8))
    } + ".signature"

    private fun ratingResponse() = DivingFishRatingResponse(
        username = USERNAME,
        rating = DEFAULT_RATING,
        additionalRating = 0,
        nickname = "测试玩家",
        charts = DivingFishCharts(sd = listOf(record()), dx = emptyList())
    )

    private fun record() = DivingFishRecord(
        achievements = 100.5,
        ds = 13.0,
        dxScore = 3366,
        fc = "fc",
        fs = "fsd",
        level = "13",
        levelIndex = 3,
        levelLabel = "Master",
        ra = 300,
        rate = "sss",
        songId = musicId,
        title = data.musics[musicId] ?.name ?: "测试曲目",
        type = "SD"
    )

    private fun MockRequestHandleScope.cached(name: String, fallback: String): HttpResponseData {
        val file = File("${data.dataPath}/$name")
        val content = runCatching {
            if (file.exists()) file.readText(UTF_8) else fallback
        }.getOrDefault(fallback)
        return respondJson(content)
    }

    private fun parseForm(body: String): Map<String, String> = body.split("&").mapNotNull { pair ->
        val key = pair.substringBefore("=", "")
        if (key.isEmpty())
            null
        else
            URLDecoder.decode(key, UTF_8) to URLDecoder.decode(pair.substringAfter("=", ""), UTF_8)
    }.toMap()

    private fun sha256Hex(input: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(UTF_8))
    )

    private fun HttpRequestData.bodyText(): String =
        (body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString() ?: ""

    private fun MockRequestHandleScope.respondJson(content: String) = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = JSON_HEADERS
    )

    private fun MockRequestHandleScope.failure(status: HttpStatusCode, message: String) = respond(
        content = """{"status":"error","message":"$message"}""",
        status = status,
        headers = JSON_HEADERS
    )

    private fun MockRequestHandleScope.tokenFailure(status: HttpStatusCode, error: String) = respond(
        content = """{"error":"$error"}""",
        status = status,
        headers = JSON_HEADERS
    )
}
