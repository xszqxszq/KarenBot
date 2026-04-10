package xyz.xszq.bot.component

import com.sksamuel.hoplite.ExperimentalHoplite
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.json.Json
import xyz.xszq.bot.add
import xyz.xszq.bot.music.ChartInfo
import xyz.xszq.bot.music.GameVersion
import xyz.xszq.bot.music.Item
import xyz.xszq.bot.music.MusicDifficulty
import xyz.xszq.bot.music.MusicGenre
import xyz.xszq.bot.music.MusicInfo
import xyz.xszq.bot.music.MusicType
import xyz.xszq.bot.payload.LocalCourseInfo
import xyz.xszq.bot.payload.LocalIconInfo
import xyz.xszq.bot.payload.LocalMusicInfo
import xyz.xszq.bot.payload.LocalPlateInfo
import xyz.xszq.bot.query.ComboQuery
import xyz.xszq.bot.query.ComboQuery.designer
import xyz.xszq.bot.query.ComboQuery.musicsPlate
import xyz.xszq.bot.query.ComboQuery.rate
import xyz.xszq.bot.query.ComboQuery.rateGreaterEqual
import xyz.xszq.bot.query.ComboQuery.version
import xyz.xszq.bot.toSimple
import java.io.File

class MaimaiData {
    val dataDirPath = "./data/maimai"

    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    val plates = mutableMapOf<Int, LocalPlateInfo>()
    val icons = mutableMapOf<Int, LocalIconInfo>()
    val courses = mutableMapOf<Int, LocalCourseInfo>()

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    lateinit var dataDir: VfsFile
    lateinit var newestVersion: GameVersion

    fun toGameVersion(name: String): GameVersion = versions[name]!!
    fun isNew(localMusicInfo: LocalMusicInfo) = localMusicInfo.version == newestVersion.name

    @OptIn(ExperimentalHoplite::class)
    fun load() {
        dataDir = localCurrentDirVfs[dataDirPath]
        getGameVersions()
        getMusicList()
        getPlateList()
        getIconsList()
        getCoursesList()
    }


    fun getMusicList(): Map<Int, MusicInfo> {
        musics.clear()
        musics.putAll(json.decodeFromString<List<LocalMusicInfo>>(
            File(dataDir.absolutePath + "/music.json").readText(Charsets.UTF_8)
        ).map { localMusicInfo ->
            MusicInfo(
                id = localMusicInfo.id,
                name = localMusicInfo.name,
                type = MusicType.of(localMusicInfo.type),
                rights = localMusicInfo.rights,
                artist = localMusicInfo.artist,
                genre = MusicGenre.of(localMusicInfo.genre),
                bpm = localMusicInfo.bpm,
                version = toGameVersion(localMusicInfo.version),
                isNew = isNew(localMusicInfo)
            ).also { info ->
                info.charts = localMusicInfo.charts.mapIndexed { index, chart ->
                    val difficulty =
                        if (info.genre == MusicGenre.Utage)
                            MusicDifficulty.Utage
                        else
                            MusicDifficulty.of(index)
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

    fun getGameVersions(): Map<String, GameVersion> {
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
}