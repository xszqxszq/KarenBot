package xyz.xszq.bot.maimai.api

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.ConnectionPool
import okhttp3.Protocol
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.database.ProberBindTable
import xyz.xszq.bot.maimai.database.QQBindTable
import xyz.xszq.bot.maimai.exception.*
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.*
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DivingFish(
    val oauthId: String,
    val oauthSecret: String,
    val maimaiData: MaimaiData,
    val client: HttpClient = createClient()
) : MaimaiAPI {
    override val id: String = "diving-fish"
    override val name: String = "水鱼"

    val server = "https://www.diving-fish.com/api/maimaidxprober"
    val authServer = "https://auth.diving-fish.com"
    val musics
        get() = maimaiData.musics

    val json = Json {
        ignoreUnknownKeys = true
    }
    private val redirectClient = HttpClient {
        followRedirects = false
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var divingFishTitleMap: Map<Int, String> = emptyMap()
        private set

    private val tokenCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()

    override suspend fun load() {
        scope.launch {
            loadMusicData()
        }
    }

    suspend fun deviceAuthorization(
        externalId: String,
        label: String
    ): DivingFishDeviceAuthorizationResponse {
        val response = client.post("$authServer/oauth/device_authorization") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(formUrlEncode(
                "client_id" to oauthId,
                "client_secret" to oauthSecret,
                "scope" to "chunithm.records.read prober.profile.read prober.records.read profile",
                "subject_ref" to subjectRef(externalId),
                "binding_label" to label
            ))
        }
        if (!response.status.isSuccess())
            throw UnknownException()
        return response.body<DivingFishDeviceAuthorizationResponse>()
    }

    suspend fun awaitDeviceBinding(
        deviceCode: String,
        intervalSeconds: Int,
        expiresInSeconds: Int
    ): Boolean {
        val deadline = System.currentTimeMillis() + expiresInSeconds * 1000L
        var intervalMillis = intervalSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            val response = client.post("$authServer/oauth/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(formUrlEncode(
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code" to deviceCode,
                    "client_id" to oauthId,
                    "client_secret" to oauthSecret
                ))
            }
            if (response.status == HttpStatusCode.OK)
                return true
            if (response.status == HttpStatusCode.BadRequest) {
                val error = runCatching {
                    response.body<JsonObject>()["error"] ?.jsonPrimitive ?.content
                }.getOrNull()
                when (error) {
                    "authorization_pending" -> {
                        delay(intervalMillis)
                        continue
                    }
                    "slow_down" -> {
                        intervalMillis *= 2
                        delay(intervalMillis)
                        continue
                    }
                    else -> return false
                }
            }
            delay(intervalMillis)
        }
        return false
    }

    suspend fun bindByRef(openid: String): String? {
        println("[水鱼调试] bindByRef openid=$openid")
        val tokens = runCatching {
            onBehalfOf("ref:${subjectRef(openid)}")
        }.getOrNull() ?: runCatching {
            val qq = QQBindTable[openid] ?: throw UnknownException()
            println("[水鱼调试] bindByRef 改用qq=$qq openid=$openid")
            onBehalfOf("ref:${subjectRef(qq.toString())}")
        }.getOrNull() ?: run {
            println("[水鱼调试] bindByRef 换票失败 openid=$openid")
            return null
        }
        val sub = decodeSub(tokens.accessToken) ?: run {
            println("[水鱼调试] bindByRef decodeSub null openid=$openid")
            return null
        }
        println("[水鱼调试] bindByRef sub=$sub openid=$openid")
        ProberBindTable[openid, "diving-fish", "id"] = sub
        runCatching {
            val data = recordsRequest(tokens.accessToken)
            ProberBindTable[openid, "diving-fish", "username"] = data.username
        }
        val expiresAt = System.currentTimeMillis() + tokens.expiresIn * 1000L
        tokenCache[openid] = Pair(tokens.accessToken, expiresAt)
        return sub
    }

    private fun subjectRef(externalId: String): String = sha256Hex("$oauthId:$externalId")

    fun clearTokenCache(id: String) {
        tokenCache.remove(id)
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
            println("[水鱼调试] onBehalfOf 429限流重试 $retry subject=$subject")
            delay(retry * 2000L)
        }
    }

    suspend fun migrateQQBindings(limit: Int = 2000): Pair<Int, Int> {
        var ok = 0
        var fail = 0
        for ((openId, qq) in QQBindTable.allBindings()) {
            if (ok + fail >= limit)
                break
            if (ProberBindTable[openId, "diving-fish", "id"] != null) {
                if (ProberBindTable[openId, "diving-fish", "username"] == null) {
                    runCatching {
                        val token = accessToken(openId) ?: throw UnknownException()
                        val data = recordsRequest(token)
                        ProberBindTable[openId, "diving-fish", "username"] = data.username
                        ok++
                    }.onFailure {
                        fail++
                    }
                    delay(1600L)
                }
                continue
            }
            if (ProberBindTable[openId, "diving-fish", "migrate-failed"] != null)
                continue
            val digest = sha256Hex("$oauthId:$qq")
            runCatching {
                val tokens = onBehalfOf("ref:$digest")
                val sub = decodeSub(tokens.accessToken) ?: return@runCatching
                ProberBindTable[openId, "diving-fish", "id"] = sub
                runCatching {
                    val data = ratingRequest(buildJsonObject {
                        put("b50", JsonPrimitive(true))
                    }, tokens.accessToken) ?: return@runCatching
                    ProberBindTable[openId, "diving-fish", "username"] = data.username
                }
                ok++
            }.onFailure { e ->
                if (e is UserBindRequiredException)
                    ProberBindTable[openId, "diving-fish", "migrate-failed"] = "1"
                fail++
            }
            delay(1600L)
        }
        return Pair(ok, fail)
    }

    private fun decodeSub(accessToken: String): String? = runCatching {
        val parts = accessToken.split(".")
        if (parts.size < 2)
            return@runCatching null
        val payload = Base64.getUrlDecoder().decode(parts[1])
        json.parseToJsonElement(payload.decodeToString())
            .jsonObject["sub"] ?.jsonPrimitive ?.content
    }.getOrNull()

    private fun sha256Hex(input: String): String =
        HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        )

    private fun formUrlEncode(
        vararg pairs: Pair<String, String>
    ): String = pairs.joinToString("&") { (key, value) ->
        "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
    }

    fun HttpRequestBuilder.setOAuth(accessToken: String) {
        headers["Authorization"] = "Bearer $accessToken"
    }

    private suspend fun resolveBindId(user: UserQueryParams): String? = when (user) {
        is UserQueryParams.Self -> {
            val openid = user.event.sender.id
            if (ProberBindTable[openid, "diving-fish", "id"] == null)
                bindByRef(openid)
            openid
        }
        is UserQueryParams.Username ->
            ProberBindTable.findIdByValue("diving-fish", "username", user.username)
        is UserQueryParams.FriendCode -> null
    }

    private fun notBoundException(user: UserQueryParams) = when (user) {
        is UserQueryParams.Username ->
            UserBindRequiredException("该用户未绑定本BOT，请该用户先绑定查分器")
        else -> UserBindRequiredException()
    }

    private suspend fun <T> withAccessToken(
        user: UserQueryParams,
        block: suspend (String) -> T
    ): T {
        println("[水鱼调试] withAccessToken user=$user")
        val openid = resolveBindId(user) ?: run {
            println("[水鱼调试] withAccessToken resolveBindId null user=$user")
            throw notBoundException(user)
        }
        println("[水鱼调试] withAccessToken openid=$openid")
        val token = accessToken(openid) ?: run {
            println("[水鱼调试] withAccessToken accessToken null openid=$openid")
            throw notBoundException(user)
        }
        var retry = 0
        while (true) {
            val result = runCatching { block(token) }
            val e = result.exceptionOrNull()
            if (e == null)
                return result.getOrThrow()
            println("[水鱼调试] withAccessToken block失败 $e retry=$retry")
            if (e is AuthorizationException) {
                tokenCache.remove(openid)
                val fresh = accessToken(openid) ?: throw UserBindRequiredException()
                if (fresh == token)
                    throw e
                return block(fresh)
            }
            if (e is UnknownException && retry < 3) {
                retry++
                delay(retry * 2000L)
                continue
            }
            throw e
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
                HttpStatusCode.Forbidden -> throw UserDeniedException()
                HttpStatusCode.BadRequest -> throw UserNotFoundException()
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

    private suspend fun <T> withUserToken(
        user: UserQueryParams,
        block: suspend (String) -> T
    ): T = withAccessToken(user) { token ->
        runCatching { block(token) }.getOrElse { e ->
            if (user is UserQueryParams.Self && e is UserNotFoundException)
                throw UserBindRequiredException()
            throw e
        }
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? = when (user) {
        is UserQueryParams.Self -> {
            println("[水鱼调试] getPlayerRating Self sender=${user.event.sender.id}")
            val data = withUserToken(user) { token ->
                ratingRequest(buildJsonObject {
                    put("b50", JsonPrimitive(true))
                }, token)
            } ?: return null
            data.toRatingResponse()
        }
        is UserQueryParams.Username -> {
            val data = ratingRequest(buildJsonObject {
                put("b50", JsonPrimitive(true))
                put("username", JsonPrimitive(user.username))
            }) ?: return null
            data.toRatingResponse()
        }
        is UserQueryParams.FriendCode -> null
    }

    private fun DivingFishRatingResponse.toRatingResponse(): RatingResponse = RatingResponse(
        player = PlayerInfo(
            nickname = nickname,
            rating = rating,
            course = additionalRating + if (additionalRating > 10) 1 else 0
        ),
        oldRatingList = charts.sd.mapNotNull { record ->
            record.toRecord()
        },
        newRatingList = charts.dx.mapNotNull { record ->
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
                recordsRequest(token, listOf(music.id)).records.mapNotNull { record ->
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
            withUserToken(user) { token ->
                val data = recordsRequest(token, musics.map { it.id })
                RecordsResponse(
                    player = PlayerInfo(
                        nickname = data.nickname,
                        rating = data.rating,
                        course = data.additionalRating + if (data.additionalRating > 10) 1 else 0
                    ),
                    records = data.records.mapNotNull { record ->
                        record.toRecord()
                    }
                )
            }
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

    suspend fun update(
        uri: String,
        importToken: String
    ): DivingFishUpdateResponse {
        val records = fetch(uri) ?: throw UnknownException()
        val response = client.post(
            "https://www.diving-fish.com/api/maimaidxprober/player/update_records"
        ) {
            headers {
                append("Import-Token", importToken)
            }
            contentType(ContentType.Application.Json)
            setBody(records)
        }
        if (!response.status.isSuccess())
            throw UnknownException()
        return response.body<DivingFishUpdateResponse>()
    }

    suspend fun fetch(
        uri: String
    ): List<DivingFishRecordSimple>? {
        val getRedirectUrl = URLBuilder("https://tgk-wcaime.wahlap.com$uri").apply {
            parameters.remove("token")
        }.build()
        val getRedirectResponse = redirectClient.get(getRedirectUrl)

        val getCookieUrl = getRedirectResponse.headers[HttpHeaders.Location] ?: return null
        val getCookieResponse = redirectClient.get(getCookieUrl)
        val cookieString = getCookieResponse.headers.getAll(HttpHeaders.SetCookie)
            ?.joinToString("; ") { it.substringBefore(";") }
            ?: return null

        val htmlResults = mutableListOf<String>()

        (0..4).forEach { difficulty ->
            val recordResponse = client.get("https://maimai.wahlap.com/maimai-mobile/record/musicSort/search/") {
                parameter("search", "A")
                parameter("sort", "1")
                parameter("playCheck", "on")
                parameter("diff", difficulty)

                header(HttpHeaders.Cookie, cookieString)
                header(HttpHeaders.UserAgent, USER_AGENT)
            }
            val content = recordResponse.bodyAsText()
            if ("错误码：" in content)
                return null
            htmlResults.add(content)
        }
        return htmlResults.flatMapIndexed { difficulty, html ->
            parseDivingFishRecords(
                html = html,
                difficulty = difficulty
            )
        }
    }


    fun parseDivingFishRecords(
        html: String,
        difficulty: Int
    ): List<DivingFishRecordSimple> = Ksoup.parse(html = html)
        .select("form[action='https://maimai.wahlap.com/maimai-mobile/record/musicDetail/']").mapNotNull { form ->
            val title = form.select(".music_name_block").firstOrNull() ?.text()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: return@mapNotNull null
            val achievement = form.select(".music_score_block.w_112").firstOrNull() ?.text()
                ?.removeSuffix("%")
                ?.trim()
                ?.toDoubleOrNull()
                ?: return@mapNotNull null
            val deluxeScoreText = form.select(".music_score_block.w_190").firstOrNull() ?.text()
                ?.trim()
                ?: return@mapNotNull null
            val deluxeScoreValues = deluxeScoreText.split("/").map(String::trim).map { value ->
                value.replace(",", "")
            }

            if (deluxeScoreValues.size != 2)
                return@mapNotNull null

            val dxScore = deluxeScoreValues[0].toIntOrNull() ?: return@mapNotNull null
            val totalDxScore = deluxeScoreValues[1].toIntOrNull() ?: return@mapNotNull null

            val type = when (form.select(".music_kind_icon").firstOrNull()
                ?.attr("src") ?.substringAfterLast('/') ?.substringBefore('.')) {
                    "music_standard" -> "SD"
                    "music_dx" -> "DX"
                    else -> return@mapNotNull null
                }
            val icons = form.select("img[src*='music_icon_']").map { icon ->
                val icon = icon.attr("src")
                    .substringAfterLast("music_icon_").substringBefore('.')
                if (icon == "back") "" else icon
            }
            val fs = icons.firstOrNull().orEmpty()
            val fc = icons.getOrNull(1).orEmpty()
            val resolvedTitle = musics.values.firstOrNull { music ->
                music.type.value == type &&
                title in music.name &&
                music.charts.getOrNull(difficulty)?.maxDeluxeScore == totalDxScore
            } ?.let { music ->
                divingFishTitleMap[music.id] ?: title
            } ?: title

            DivingFishRecordSimple(
                title = resolvedTitle,
                achievements = achievement,
                dxScore = dxScore,
                fc = fc,
                fs = fs,
                levelIndex = difficulty,
                type = type
            )
    }

    fun DivingFishRecord.toRecord(): Record? {
        val music = musics[songId] ?: return null
        val chart = if (music.genre == MusicGenre.Utage)
            music.charts[0]
        else
            music.charts[levelIndex]
        val achievement = (achievements * 10000).toInt()
        return Record(
            music = music,
            chart = chart,
            achievement = achievement,
            comboStatus = ComboStatus.of(fc),
            syncStatus = SyncStatus.of(fs),
            deluxeScore = dxScore,
            rate = rate,
            rating = Rating.calc(chart, achievement)
        )
    }

    suspend fun getStats(): DivingFishStats = client.get("$server/chart_stats").body<DivingFishStats>()

    suspend fun loadMusicData() {
        val cacheFile = File("${maimaiData.dataPath}/diving-fish.json")
        val cached = runCatching {
            if (cacheFile.exists())
                json.decodeFromString<List<DivingFishMusicInfo>>(cacheFile.readText(Charsets.UTF_8))
            else null
        }.getOrNull()
        if (cached != null) {
            divingFishTitleMap = cached.associate { m ->
                m.id.toInt() to m.title
            }
        }
        runCatching {
            val musics: List<DivingFishMusicInfo> = client.get("$server/music_data").body()
            divingFishTitleMap = musics.associate { m ->
                m.id.toInt() to m.title
            }
            cacheFile.parentFile ?.mkdirs()
            cacheFile.writeText(
                json.encodeToString(musics),
                Charsets.UTF_8
            )
        }
    }

    fun getDivingFishTitle(musicId: Int, originalTitle: String): String =
        divingFishTitleMap[musicId] ?: originalTitle

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/132.0.0.0 Safari/537.36 NetType/WIFI MicroMessenger/7.0.20.1781(0x6700143B) " +
                "WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf254162e) XWEB/18163 Flue"

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