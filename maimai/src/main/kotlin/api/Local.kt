package xyz.xszq.bot.api

import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.json.Json
import xyz.xszq.bot.add
import xyz.xszq.bot.component.LocalConnector
import xyz.xszq.bot.component.Tag
import xyz.xszq.bot.database.MaimaiBindTable
import xyz.xszq.bot.database.MaimaiSettingsTable
import xyz.xszq.bot.event.MessageEvent
import xyz.xszq.bot.music.*
import xyz.xszq.bot.payload.*
import xyz.xszq.bot.query.Query
import xyz.xszq.bot.query.Query.designer
import xyz.xszq.bot.query.Query.musicsPlate
import xyz.xszq.bot.query.Query.rate
import xyz.xszq.bot.query.Query.rateGreaterEqual
import xyz.xszq.bot.query.Query.version
import xyz.xszq.bot.toSimple
import java.io.File

class Local(
    val connector: LocalConnector
): MaimaiAPI {
    override val name: String = "local"

    val dataDirPath = "./data/maimai"

    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    val plates = mutableMapOf<Int, LocalPlateInfo>()
    val icons = mutableMapOf<Int, LocalIconInfo>()
    val courses = mutableMapOf<Int, LocalCourseInfo>()

    val json = Json {
        ignoreUnknownKeys = true
    }

    lateinit var dataDir: VfsFile
    lateinit var newestVersion: GameVersion

    fun toGameVersion(name: String): GameVersion = versions[name]!!
    fun isNew(localMusicInfo: LocalMusicInfo) = localMusicInfo.version == newestVersion.name

    override suspend fun load() {
        dataDir = localCurrentDirVfs[dataDirPath]
        getGameVersions()
        getMusicList()
        getPlateList()
        getIconsList()
        getCoursesList()

        plates.values.filter {
            it.genre == "実績" && it.requires.isNotEmpty() && it.name != "覇者"
        }.associateBy {
            it.name.replace(Item.plateTypes.first { type -> it.name.endsWith(type) }, "")
        }.also { filtered ->
            val early = filtered.filter { (version, _) ->
                version in listOf("真", "超", "檄")
            }.flatMap { (_, plate) ->
                plate.requires.mapNotNull { musics[it] ?.version }.toSet().toList()
            }.toSet().toList()
            Query.conditions.add(0, Pair(listOf("真超檄"),version(early)))
        }.forEach { (version, plate) ->
            val simplified = Item.simplifyTable[version] ?: version.toSimple()
            val gameVersions = plate.requires.mapNotNull { musics[it] ?.version }.toSet().toList()
            Query.conditions.add(Pair(listOf(version, simplified),
                version(gameVersions)))
            Query.conditions.add(0, Pair(listOf(simplified + "代"),
                version(gameVersions)))
        }
        versions.values.filter { it.version > 20000 }.forEach { version ->
            val year = version.name.substringAfter("舞萌DX ")
            Query.conditions.add(0, Pair(listOf("dx$year", year), Query.nowVersion(version)))
        }
        versions.values.firstOrNull { it.version == 20000 } ?.let { version ->
            Query.conditions.add(0, Pair(listOf("dx无印"), Query.nowVersion(version)))
        }
        Query.conditions.add(Pair(listOf("标准", "标"), Query.type(MusicType.Standard)))
        Query.conditions.add(Pair(listOf("dx谱"), Query.type(MusicType.Deluxe)))
        Query.conditions.add(Pair(listOf("旧框"),
            version(versions.values.filter { it.version <= 19900 })
        ))
        Query.conditions.add(Pair(listOf("dx"),
            version(versions.values.filter { it.version > 19900 })
        ))
        Query.conditions.add(Pair(listOf("旧版本", "旧"),
            version(versions.values.filter { it != newestVersion })
        ))
        Query.conditions.add(Pair(listOf("新版本", "新歌", "新"),
            version(listOf(newestVersion))
        ))
        plates.values.filter {
            it.genre == "実績" && it.requires.isNotEmpty()
        }.forEach { plate ->
            val name = Item.toSimplified(plate.name)
            Query.conditions.add(0, Pair(listOf(plate.name, name),
                musicsPlate(plate.requires, plate.remasters, plate.name)))
        }
        musics.values.flatMap { music ->
            music.charts.map { chart -> chart.notesDesigner }
        }.toSet().toList().forEach { designer ->
            if (designer.isNotBlank() && designer != "-")
                Query.conditions.add(0, Pair(listOf(designer), designer(designer)))
        }
        Query.conditions.add(listOf("纯ss+", "仅ss+"), rate("ssp"))
        Query.conditions.add(listOf("纯ss", "仅ss"), rate("ss"))
        Query.conditions.add(listOf("纯s+", "仅s+"), rate("sp"))
        Query.conditions.add(listOf("纯s", "仅s"), rate("s"))
        Query.conditions.add(listOf("纯aaa", "仅aaa"), rate("aaa"))
        Query.conditions.add(listOf("ss+", "ssp"), rateGreaterEqual("ssp"))
        Query.conditions.add(listOf("ss", "ss"), rateGreaterEqual("ss"))
        Query.conditions.add(listOf("s+", "sp"), rateGreaterEqual("sp"))
        Query.conditions.add(listOf("s"), rateGreaterEqual("s"))
        Query.conditions.add(listOf("aaa"), rateGreaterEqual("aaa"))

        val customTags = json.decodeFromString<Map<String, Tag>>(
            File(dataDir.absolutePath + "/tag.json").readText(Charsets.UTF_8)
        )
        customTags.forEach { (_, tag) ->
            Query.conditions.add(Pair(tag.aliases, Query.tag(tag.musics, tag.name)))
        }
        Query.compile()
    }

    override suspend fun getMusicList(): Map<Int, MusicInfo> {
        musics.clear()
        val ngMusics = runCatching {
            connector.ngMusics()
        }.getOrNull() ?: emptyList()
        musics.putAll(json.decodeFromString<List<LocalMusicInfo>>(
            File(dataDir.absolutePath + "/music.json").readText(Charsets.UTF_8)
        ).filter { it.id !in ngMusics }.map { localMusicInfo ->
            MusicInfo(
                id = localMusicInfo.id,
                name = localMusicInfo.name,
                type = MusicType.Companion.of(localMusicInfo.type),
                rights = localMusicInfo.rights,
                artist = localMusicInfo.artist,
                genre = MusicGenre.Companion.of(localMusicInfo.genre),
                bpm = localMusicInfo.bpm,
                version = toGameVersion(localMusicInfo.version),
                isNew = isNew(localMusicInfo)
            ).also { info ->
                info.charts = localMusicInfo.charts.mapIndexed { index, chart ->
                    val difficulty =
                        if (info.genre == MusicGenre.Utage)
                            MusicDifficulty.Utage
                        else
                            MusicDifficulty.Companion.of(index)
                    ChartInfo(
                        music = info,
                        difficulty = difficulty,
                        level = chart.level,
                        levelValue = chart.levelValue,
                        notes = chart.notes,
                        notesDesigner = chart.notesDesigner
                    )
                }
            }
        }.associateBy { it.id })
        return musics
    }

    override suspend fun getGameVersions(): Map<String, GameVersion> {
        versions.clear()
        versions.putAll(json.decodeFromString<List<GameVersion>>(
            File(dataDir.absolutePath + "/version.json").readText(Charsets.UTF_8)
        ).also { list ->
            newestVersion = list.last()
        }.associateBy { it.name })
        return versions
    }

    fun getPlateList(): Map<Int, LocalPlateInfo> {
        plates.clear()
        plates.putAll(json.decodeFromString<List<LocalPlateInfo>>(
            File(dataDir.absolutePath + "/plate.json").readText(Charsets.UTF_8)
        ).associateBy { it.id })
        return plates
    }

    fun getIconsList(): Map<Int, LocalIconInfo> {
        icons.clear()
        icons.putAll(json.decodeFromString<List<LocalIconInfo>>(
            File(dataDir.absolutePath + "/icon.json").readText(Charsets.UTF_8)
        ).associateBy { it.id })
        return icons
    }

    fun getCoursesList(): Map<Int, LocalCourseInfo> {
        courses.clear()
        courses.putAll(json.decodeFromString<List<LocalCourseInfo>>(
            File(dataDir.absolutePath + "/course.json").readText(Charsets.UTF_8)
        ).associateBy { it.id })
        return courses
    }

    override suspend fun getPlayerRating(
        event: MessageEvent,
        args: String
    ): RatingResponse? {
        val userId =
            if (args.isNotBlank()) (args.toLongOrNull() ?: return null)
            else MaimaiBindTable[event.sender.id] ?: return null
        val uid = userId
        val playerInfo = connector.info(uid)
        val ratingResponse = connector.rating(uid)
        val records = connector.musics(uid).associateBy { Pair(it.musicId, it.level) }
        return RatingResponse(
            name = playerInfo.userName,
            rating = playerInfo.playerRating,
            course = 0,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: playerInfo.iconId,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?: 11,
            ratingList = ratingResponse.ratingList.filter { it.achievement != 0 }.mapNotNull { rating ->
                records[Pair(rating.musicId, rating.level)]!!.toRecord()
            },
            newRatingList = ratingResponse.newRatingList.filter { it.achievement != 0 }.mapNotNull { rating ->
                records[Pair(rating.musicId, rating.level)]!!.toRecord()
            }
        )
    }

    override suspend fun getPlayerRecord(
        event: MessageEvent,
        args: String,
        music: MusicInfo
    ): List<Record>? {
        val userId =
            if (args.isNotBlank()) (args.toLongOrNull() ?: return null)
            else MaimaiBindTable[event.sender.id] ?: return null
        val records = connector.musics(userId)

        return records.filter { it.musicId == music.id }.mapNotNull { it.toRecord() }
    }

    override suspend fun getPlayerRecords(
        event: MessageEvent,
        args: String,
        musics: List<MusicInfo>
    ): RecordsResponse? {
        val ids = musics.map { it.id }
        val userId =
            if (args.isNotBlank()) (args.toLongOrNull() ?: return null)
            else MaimaiBindTable[event.sender.id] ?: return null
        val playerInfo = connector.info(userId)
        val records = connector.musics(userId)

        return RecordsResponse(
            name = playerInfo.userName,
            rating = playerInfo.playerRating,
            course = 0,
            icon = MaimaiSettingsTable[event.sender.id, "icon"] ?.toIntOrNull() ?: playerInfo.iconId,
            plate = MaimaiSettingsTable[event.sender.id, "plate"] ?.toIntOrNull() ?: 11,
            records = records.filter { it.musicId in ids }.mapNotNull { it.toRecord() }
        )
    }

    fun MaimaiRecord.toRecord(): Record? {
        val music = musics[musicId] ?: return null
        val chart = if (music.genre == MusicGenre.Utage)
            music.charts[0]
        else
            music.charts[level]
        return Record(
            music = music,
            chart = chart,
            achievement = achievement,
            comboStatus = ComboStatus.Companion.of(comboStatus),
            syncStatus = SyncStatus.Companion.of(syncStatus),
            deluxeScore = deluxeScoreMax,
            rate = Rate[achievement],
            rating = Rating.calc(chart, achievement)
        )
    }
}