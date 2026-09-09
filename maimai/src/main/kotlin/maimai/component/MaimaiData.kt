package xyz.xszq.bot.maimai.component

import com.sksamuel.hoplite.ExperimentalHoplite
import korlibs.io.file.VfsFile
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.serialization.json.Json
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.payload.LocalCourseInfo
import xyz.xszq.bot.maimai.payload.LocalIconInfo
import xyz.xszq.bot.maimai.payload.LocalMusicInfo
import xyz.xszq.bot.maimai.payload.LocalPlateInfo
import java.io.File

/**
 * 舞萌游戏数据
 */
class MaimaiData(
    val dataPath: String = "./data/maimai"
) {

    val versions = mutableMapOf<String, GameVersion>()
    val musics = mutableMapOf<Int, MusicInfo>()
    var localMusics: List<LocalMusicInfo> = emptyList()
    val plates = mutableMapOf<Int, LocalPlateInfo>()
    val icons = mutableMapOf<Int, LocalIconInfo>()
    val courses = mutableMapOf<Int, LocalCourseInfo>()

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    lateinit var dataDir: VfsFile
    lateinit var newestVersion: GameVersion

    /**
     * 按名称获取版本
     *
     * @param name 版本名称
     * @return 对应的版本
     */
    fun toGameVersion(name: String): GameVersion = versions[name]!!

    /**
     * 本地曲目是否属于最新版本
     */
    fun isNew(localMusicInfo: LocalMusicInfo) = localMusicInfo.version == newestVersion.name

    /**
     * 载入本地数据
     */
    @OptIn(ExperimentalHoplite::class)
    fun load() {
        dataDir = localCurrentDirVfs[dataPath]
        getGameVersions()
        getMusicList()
        getPlateList()
        getIconsList()
        getCoursesList()
    }


    /**
     * 载入歌曲信息
     *
     * @return 歌曲列表
     */
    fun getMusicList(): Map<Int, MusicInfo> {
        musics.clear()
        val decoded = json.decodeFromString<List<LocalMusicInfo>>(
            File(dataDir.absolutePath + "/music.json").readText(Charsets.UTF_8)
        )
        localMusics = decoded
        musics.putAll(decoded.map { localMusicInfo ->
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

    /**
     * 载入版本信息
     *
     * @return 版本列表
     */
    fun getGameVersions(): Map<String, GameVersion> {
        versions.clear()
        versions.putAll(json.decodeFromString<List<GameVersion>>(
            File(dataDir.absolutePath + "/version.json").readText(Charsets.UTF_8)
        ).also { list ->
            newestVersion = list.last()
        }.associateBy { it.name })
        return versions
    }

    /**
     * 载入牌子信息
     *
     * @return 牌子列表
     */
    fun getPlateList(): Map<Int, LocalPlateInfo> {
        plates.clear()
        plates.putAll(json.decodeFromString<List<LocalPlateInfo>>(
            File(dataDir.absolutePath + "/plate.json").readText(Charsets.UTF_8)
        ).associateBy { it.id })
        return plates
    }

    /**
     * 载入头像信息
     *
     * @return 头像列表
     */
    fun getIconsList(): Map<Int, LocalIconInfo> {
        icons.clear()
        icons.putAll(json.decodeFromString<List<LocalIconInfo>>(
            File(dataDir.absolutePath + "/icon.json").readText(Charsets.UTF_8)
        ).associateBy { it.id })
        return icons
    }

    /**
     * 载入段位信息
     *
     * @return 段位列表
     */
    fun getCoursesList(): Map<Int, LocalCourseInfo> {
        courses.clear()
        courses.putAll(json.decodeFromString<List<LocalCourseInfo>>(
            File(dataDir.absolutePath + "/course.json").readText(Charsets.UTF_8)
        ).associateBy { it.id })
        return courses
    }
}