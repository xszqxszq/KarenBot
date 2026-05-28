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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.exception.*
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DivingFish(
    val token: String,
    val maimaiData: MaimaiData
) : MaimaiAPI {
    override val id: String = "diving-fish"
    override val name: String = "水鱼"

    val server = "https://www.diving-fish.com/api/maimaidxprober"
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
            retryOnExceptionIf { request, response ->
                request.method == HttpMethod.Post
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
    }
    private val redirectClient = HttpClient {
        followRedirects = false
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var divingFishTitleMap: Map<Int, String> = emptyMap()
        private set

    override suspend fun load() {
        scope.launch {
            loadMusicData()
        }
    }

    override suspend fun getPlayerRating(
        user: UserQueryParams
    ): RatingResponse? {
        val request = buildRequest(user)
        val data = ratingRequest(request) ?: return null

        return RatingResponse(
            player = PlayerInfo(
                nickname = data.nickname,
                rating = data.rating,
                course = data.additionalRating + if (data.additionalRating > 10) 1 else 0
            ),
            oldRatingList = data.charts.sd.mapNotNull { record ->
                record.toRecord()
            },
            newRatingList = data.charts.dx.mapNotNull { record ->
                record.toRecord()
            }
        )
    }

    override suspend fun getPlayerRecord(
        user: UserQueryParams,
        music: MusicInfo
    ): List<Record>? {
        val ids = listOf(music.id)
        return getRecordsDeveloper(user, ids, true) ?.records
    }

    override suspend fun getPlayerRecords(
        user: UserQueryParams,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        val ids = musics.map { it.id }
        return getRecordsDeveloper(user, ids)
    }

    suspend fun getRecordsDeveloper(
        user: UserQueryParams,
        ids: List<Int>,
        simple: Boolean = false
    ): RecordsResponse? {
        val request = buildRequest(user) {
            putJsonArray("music_id") {
                ids.forEach { id ->
                    add(JsonPrimitive(id.toString()))
                }
            }
        }
        val data = recordRequestDeveloper(request).mapNotNull { it.toRecord() }

        return getRecordsResponse(user, simple, data)
    }

    suspend fun getRecordsResponse(
        user: UserQueryParams,
        simple: Boolean = false,
        data: List<Record>
    ): RecordsResponse? = if (simple) {
        RecordsResponse(
            player = PlayerInfo(),
            records = data
        )
    } else {
        val request = buildRequest(user)
        val basicInfo = ratingRequest(request) ?: return null
        RecordsResponse(
            player = PlayerInfo(
                nickname = basicInfo.nickname,
                rating = basicInfo.rating,
                course = basicInfo.additionalRating + if (basicInfo.additionalRating > 10) 1 else 0
            ),
            records = data
        )
    }

    suspend fun ratingRequest(request: JsonObject): DivingFishRatingResponse? {
        val response = client.post("$server/query/player") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
            HttpStatusCode.OK -> response.body<DivingFishRatingResponse>()
            else -> {
                println(response)
                throw UnknownException()
            }
        }
    }

    suspend fun recordRequestDeveloper(request: JsonObject): List<DivingFishRecord> {
        val response = client.post("$server/dev/player/record") {
            contentType(ContentType.Application.Json)
            setDeveloper()
            setBody(request)
        }

        return when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
            HttpStatusCode.OK -> response.body<Map<String, List<DivingFishRecord>>>().values.flatten()
            else -> {
                println(response)
                throw UnknownException()
            }
        }
    }

    fun HttpRequestBuilder.setDeveloper() {
        headers["developer-token"] = token
    }

    fun buildRequest(
        user: UserQueryParams,
        additional: JsonObjectBuilder.() -> Unit = {}
    ): JsonObject {
        val request = buildJsonObject {
            put("b50", JsonPrimitive(true))
            when (user) {
                is UserQueryParams.QQ -> put("qq", JsonPrimitive(user.qq))
                is UserQueryParams.Username -> put("username", JsonPrimitive(user.username))
            }
            additional()
        }
        return request
    }

    suspend fun loadMusicData() {
        val cacheFile = File("./data/maimai/diving-fish.json")
        val cached = runCatching {
            if (cacheFile.exists())
                json.decodeFromString<DivingFishMusicCache>(cacheFile.readText(Charsets.UTF_8))
            else null
        }.getOrNull()
        if (cached != null) {
            divingFishTitleMap = cached.musics.associate { m ->
                m.id.toInt() to m.title
            }
        } else if (cacheFile.exists()) {
            runCatching {
                val musics = json.decodeFromString<List<DivingFishMusicInfo>>(cacheFile.readText(Charsets.UTF_8))
                divingFishTitleMap = musics.associate { m ->
                    m.id.toInt() to m.title
                }
            }
        }
        val etag = cached ?.etag ?: ""
        runCatching {
            val response = client.get("$server/music_data") {
                if (etag.isNotBlank())
                    header(HttpHeaders.IfNoneMatch, "\"$etag\"")
            }
            if (response.status == HttpStatusCode.OK) {
                val newEtag = response.headers[HttpHeaders.ETag] ?.removeSurrounding("\"") ?: ""
                val musics: List<DivingFishMusicInfo> = response.body()
                divingFishTitleMap = musics.associate { m ->
                    m.id.toInt() to m.title
                }
                cacheFile.parentFile ?.mkdirs()
                cacheFile.writeText(
                    json.encodeToString(DivingFishMusicCache(etag = newEtag, musics = musics)),
                    Charsets.UTF_8
                )
            }
        }
    }

    fun getDivingFishTitle(musicId: Int, originalTitle: String): String =
        divingFishTitleMap[musicId] ?: originalTitle

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

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/132.0.0.0 Safari/537.36 NetType/WIFI MicroMessenger/7.0.20.1781(0x6700143B) " +
                "WindowsWechat(0x63090a13) UnifiedPCWindowsWechat(0xf254162e) XWEB/18163 Flue"
    }
}
