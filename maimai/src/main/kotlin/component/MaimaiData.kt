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
            ComboQuery.conditions.add(0, Pair(listOf("真超檄"),version(early)))
        }.forEach { (version, plate) ->
            val simplified = Item.simplifyTable[version] ?: version.toSimple()
            val gameVersions = plate.requires.mapNotNull { musics[it] ?.version }.toSet().toList()
            ComboQuery.conditions.add(Pair(listOf(version, simplified),
                version(gameVersions)))
            ComboQuery.conditions.add(0, Pair(listOf(simplified + "代"),
                version(gameVersions)))
        }
        versions.values.filter { it.version > 20000 }.forEach { version ->
            val year = version.name.substringAfter("舞萌DX ")
            ComboQuery.conditions.add(0, Pair(listOf("dx$year", year), ComboQuery.nowVersion(version)))
        }
        versions.values.firstOrNull { it.version == 20000 } ?.let { version ->
            ComboQuery.conditions.add(0, Pair(listOf("dx无印"), ComboQuery.nowVersion(version)))
        }
        ComboQuery.conditions.add(Pair(listOf("标准", "标"), ComboQuery.type(MusicType.Standard)))
        ComboQuery.conditions.add(Pair(listOf("dx谱"), ComboQuery.type(MusicType.Deluxe)))
        ComboQuery.conditions.add(Pair(listOf("旧框"),
            version(versions.values.filter { it.version <= 19900 })
        ))
        ComboQuery.conditions.add(Pair(listOf("dx"),
            version(versions.values.filter { it.version > 19900 })
        ))
        ComboQuery.conditions.add(Pair(listOf("旧版本", "旧"),
            version(versions.values.filter { it != newestVersion })
        ))
        ComboQuery.conditions.add(Pair(listOf("新版本", "新歌", "新"),
            version(listOf(newestVersion))
        ))
        plates.values.filter {
            it.genre == "実績" && it.requires.isNotEmpty()
        }.forEach { plate ->
            val name = Item.toSimplified(plate.name)
            ComboQuery.conditions.add(0, Pair(listOf(plate.name, name),
                musicsPlate(plate.requires, plate.remasters, plate.name)))
        }
        musics.values.flatMap { music ->
            music.charts.map { chart -> chart.notesDesigner }
        }.toSet().toList().forEach { designer ->
            if (designer.isNotBlank() && designer != "-")
                ComboQuery.conditions.add(0, Pair(listOf(designer), designer(designer)))
        }
        ComboQuery.conditions.add(listOf("纯ss+", "仅ss+"), rate("ssp"))
        ComboQuery.conditions.add(listOf("纯ss", "仅ss"), rate("ss"))
        ComboQuery.conditions.add(listOf("纯s+", "仅s+"), rate("sp"))
        ComboQuery.conditions.add(listOf("纯s", "仅s"), rate("s"))
        ComboQuery.conditions.add(listOf("纯aaa", "仅aaa"), rate("aaa"))
        ComboQuery.conditions.add(listOf("ss+", "ssp"), rateGreaterEqual("ssp"))
        ComboQuery.conditions.add(listOf("ss", "ss"), rateGreaterEqual("ss"))
        ComboQuery.conditions.add(listOf("s+", "sp"), rateGreaterEqual("sp"))
        ComboQuery.conditions.add(listOf("s"), rateGreaterEqual("s"))
        ComboQuery.conditions.add(listOf("aaa"), rateGreaterEqual("aaa"))

        val customTags = json.decodeFromString<Map<String, Tag>>(
            File(dataDir.absolutePath + "/tag.json").readText(Charsets.UTF_8)
        )
        customTags.forEach { (_, tag) ->
            ComboQuery.conditions.add(Pair(tag.aliases, ComboQuery.tag(tag.musics, tag.name)))
        }
        ComboQuery.compile()
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