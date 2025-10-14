package xyz.xszq.bot.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import xyz.xszq.bot.api.exception.UnknownException
import xyz.xszq.bot.api.exception.UserDeniedException
import xyz.xszq.bot.api.exception.UserBindRequiredException
import xyz.xszq.bot.api.exception.UserNotFoundException
import xyz.xszq.bot.database.MaimaiSettingsTable
import xyz.xszq.bot.database.QQBindTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.music.*
import xyz.xszq.bot.payload.*

class DivingFish(
    val token: String,
    val local: Local
) : MaimaiAPI {
    override val name: String = "diving-fish"

    val server = "https://www.diving-fish.com/api/maimaidxprober"
    val musics
        get() = local.musics
    val divingFishVersions = mutableMapOf<String, GameVersion>()

    val json = Json {
        ignoreUnknownKeys = true
    }
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun load() {
//        musics.clear()
//        musics.putAll(getMusicList())
    }

    val jpn2chn = buildMap {
        put("maimai でらっくす", "舞萌DX")
        put("maimai でらっくす Splash", "舞萌DX 2021")
        put("maimai でらっくす UNiVERSE", "舞萌DX 2022")
        put("maimai でらっくす FESTiVAL", "舞萌DX 2023")
        put("maimai でらっくす BUDDiES", "舞萌DX 2024")
    }

    fun toNotes(list: List<Int>): Notes = when (list.size) {
        4 -> Notes(list[0], list[1], list[2], 0, list[3])
        5 -> Notes(list[0], list[1], list[2], list[3], list[4])
        else -> throw IllegalStateException()
    }

    fun toGameVersion(name: String): GameVersion {
        if (name.startsWith("maimai でらっくす"))
            return local.versions[jpn2chn[name]!!]!!
        return local.versions[name] ?: local.versions[name.replace("maimai", "").trim()]!!
    }

    fun parsePlate(name: String): Int = local.plates.values.firstOrNull {
        it.name == name
    } ?.id ?: 11

    override suspend fun getMusicList(): Map<Int, MusicInfo> {
        val data = client.get("$server/music_data").body<List<DivingFishMusicInfo>>()

        divingFishVersions.clear()
        divingFishVersions.putAll(data.map {
            it.basicInfo.from
        }.toSet().toList().associateWith {
            toGameVersion(it)
        })

        return data.map { musicInfo ->
            MusicInfo(
                id = musicInfo.id.toInt(),
                name = musicInfo.title,
                type = MusicType.Companion.of(musicInfo.type),
                rights = "",
                artist = musicInfo.basicInfo.artist,
                genre = MusicGenre.Companion.fromDivingFish(musicInfo.basicInfo.genre),
                bpm = musicInfo.basicInfo.bpm,
                version = divingFishVersions[musicInfo.basicInfo.from]!!,
                isNew = musicInfo.basicInfo.isNew
            ).also { info ->
                info.charts = musicInfo.charts.mapIndexed { index, chart ->
                    val difficulty =
                        if (info.genre == MusicGenre.Utage)
                            MusicDifficulty.Utage
                        else
                            MusicDifficulty.Companion.of(index)
                    ChartInfo(
                        music = info,
                        difficulty = difficulty,
                        level = musicInfo.level[index],
                        levelValue = musicInfo.ds[index],
                        notes = toNotes(chart.notes),
                        notesDesigner = chart.charter
                    )
                }
            }
        }.associateBy { it.id }
    }

    override suspend fun getGameVersions(): Map<String, GameVersion> = divingFishVersions

    override suspend fun getPlayerRating(event: MessageEvent, args: String): RatingResponse? {
        val request = buildRequest(event, args) ?: return null
        val data = ratingRequest(request) ?: return null

        return RatingResponse(
            name = data.nickname,
            rating = data.rating,
            course = data.additionalRating + if (data.additionalRating > 10) 1 else 0,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: 101,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?:
                data.plate ?.let { parsePlate(it) } ?: 11,
            ratingList = data.charts.sd.mapNotNull { record ->
                record.toRecord()
            },
            newRatingList = data.charts.dx.mapNotNull { record ->
                record.toRecord()
            }
        )
    }

    override suspend fun getPlayerRecord(
        event: MessageEvent,
        args: String,
        music: MusicInfo
    ): List<Record>? {
        val ids = listOf(music.id)
        return getRecordsDeveloper(ids, event, args, true) ?.records
    }

    override suspend fun getPlayerRecords(
        event: MessageEvent,
        args: String,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        val ids = musics.map { it.id }
        return getRecordsDeveloper(ids, event, args)
    }

    suspend fun getRecordsDeveloper(
        ids: List<Int>,
        event: MessageEvent,
        args: String,
        simple: Boolean = false
    ): RecordsResponse? {
        val request = buildRequest(event, args) {
            putJsonArray("music_id") {
                ids.forEach { id ->
                    add(JsonPrimitive(id.toString()))
                }
            }
        } ?: return null
        val data = recordRequestDeveloper(request).mapNotNull { it.toRecord() }

        return getRecordsResponse(event, args, simple, data)
    }

    suspend fun getRecordsByPlate(
        ids: List<Int>,
        event: MessageEvent,
        args: String,
        simple: Boolean = false
    ): RecordsResponse? {
        val request = buildRequest(event, args) {
            putJsonArray("version") {
                divingFishVersions.keys.forEach { version ->
                    add(JsonPrimitive(version))
                }
            }
        } ?: return null
        val data = recordsRequest(request)
            ?.verList?.filter { it.id in ids }?.mapNotNull { it.toRecord() }
            ?: return null

        return getRecordsResponse(event, args, simple, data)
    }

    suspend fun getRecordsResponse(
        event: MessageEvent,
        args: String,
        simple: Boolean = false,
        data: List<Record>
    ): RecordsResponse? = if (simple) {
        RecordsResponse(
            name = "",
            rating = 0,
            course = 0,
            icon = 0,
            plate = 0,
            records = data
        )
    } else {
        val request = buildRequest(event, args) ?: return null
        val basicInfo = ratingRequest(request) ?: return null
        RecordsResponse(
            name = basicInfo.nickname,
            rating = basicInfo.rating,
            course = basicInfo.additionalRating + if (basicInfo.additionalRating > 10) 1 else 0,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: 101,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?:
                basicInfo.plate ?.let { parsePlate(it) } ?: 11,
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
            else -> throw UnknownException()
        }
    }

    suspend fun recordsRequest(request: JsonObject): DivingFishPlateResponse? {
        val response = client.post("$server/query/plate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        return when (response.status) {
            HttpStatusCode.BadRequest -> throw UserNotFoundException()
            HttpStatusCode.Forbidden -> throw UserDeniedException()
            HttpStatusCode.OK -> response.body<DivingFishPlateResponse>()
            else -> throw UnknownException()
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
            else -> throw UnknownException()
        }
    }

    fun HttpRequestBuilder.setDeveloper() {
        headers["developer-token"] = token
    }

    suspend fun buildRequest(
        event: MessageEvent,
        args: String,
        additional: JsonObjectBuilder.() -> Unit = {}
    ): JsonObject? {
        val request = buildJsonObject {
            put("b50", JsonPrimitive(true))
            if (args.isEmpty()) {
                val qq = QQBindTable[event.sender.id] ?: throw UserBindRequiredException()
                put("qq", JsonPrimitive(qq))
            } else {
                put("username", JsonPrimitive(args))
            }
            additional()
        }
        return request
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
            comboStatus = ComboStatus.Companion.of(fc),
            syncStatus = SyncStatus.Companion.of(fs),
            deluxeScore = dxScore,
            rate = rate,
            rating = Rating.calc(chart, achievement)
        )
    }

    fun DivingFishPlateRecord.toRecord(): Record? {
        val music = musics[id] ?: return null
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
            deluxeScore = 0,
            rate = Rate[achievement],
            rating = Rating.calc(chart, achievement)
        )
    }
}