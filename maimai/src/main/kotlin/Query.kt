package xyz.xszq.bot

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.util.toStringDecimal
import korlibs.math.toIntRound
import xyz.xszq.bot.config.DesignerConfig
import xyz.xszq.bot.music.*
import kotlin.random.Random

object Query {
    @OptIn(ExperimentalHoplite::class)
    val designerConfig = ConfigLoaderBuilder.Companion.default()
        .addFileSource("./config/maimai-designer.yml")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<DesignerConfig>()

    val ap = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.comboStatus.isAP()
    })
    val app = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.comboStatus == ComboStatus.AllPerfectPlus
    })
    val fc = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.comboStatus.isFC()
    })
    val fsd = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.syncStatus.isFSD()
    })
    val fsdp = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.syncStatus == SyncStatus.FullSyncDeluxePlus
    })
    val close = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        val decimal = record.achievement % 10000
        record.achievement in 995000..1005000
                && (decimal in 4250..4999 || decimal in 9250..9999)
    }, sortBy = { record ->
        var target = (record.achievement.toDouble() / 10000).toIntRound() * 10000
        if (target < record.achievement)
            target += 5000
        target - record.achievement
    })
    val just = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        val decimal = record.achievement % 10000
        record.achievement in 1000000..1005000
                && (decimal in 5000..5250 || decimal in 0..1250)
    }, sortBy = { record ->
        var target = (record.achievement.toDouble() / 10000).toIntRound() * 10000
        if (target > record.achievement)
            target -= 5000
        record.achievement - target
    })
    val noB15 = Filter(disable15 = true)
    val fitLevelValues = Filter(fitLevelValues = true, sortBy = { record ->
        -Rating.calc(record.chart.fitLevelValue, record.achievement)
    })
    fun random(random: Random) = Filter(sortBy = { record ->
        random.nextInt()
    })
    fun rate(rate: String) = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        Rate.greaterEqual(record.achievement, rate)
    })
    fun achievement(achievement: Int) = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.achievement >= achievement
    })
    fun achievementLess(achievement: Int) = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage
    }, record = { record ->
        record.achievement < achievement
    })
    fun difficulty(difficulty: MusicDifficulty) = Filter(chart = { chart ->
        chart.difficulty == difficulty
    })
    fun genre(genre: MusicGenre) = Filter(chart = { chart ->
        chart.music.genre == genre
    })
    fun level(level: String) = Filter(chart = { chart ->
        chart.level == level
    }, name = "level")
    fun levelValue(levelValue: Double) = Filter(chart = { chart ->
        chart.difficulty != MusicDifficulty.Utage && chart.levelValue == levelValue
    }, name = "levelValue")
    fun designer(designer: String) = Filter(chart = { chart ->
        (chart.notesDesigner.lowercase() == designer.lowercase() ||
                designerConfig.includes[designer] ?.let { chart.notesDesigner in it } == true ||
                designerConfig.collabs[designer] ?.let { c -> c.any { raw ->
                    val nowId = raw.substringBefore("#").toInt()
                    val nowDiff = MusicDifficulty.of(raw.substringAfter("#").toInt())
                    chart.music.id == nowId && chart.difficulty == nowDiff
                } } == true ||
                designer.lowercase() in chart.notesDesigner.lowercase())
    })
    fun musics(musics: List<Int>, reMasters: List<Int>, name: String? = null) = Filter(chart = { chart ->
        if (chart.difficulty == MusicDifficulty.ReMaster)
            chart.music.id in reMasters
        else
            chart.music.id in musics
    }, name = name)
    fun musicsPlate(musics: List<Int>, reMasters: List<Int>, plateName: String) = Filter(chart = { chart ->
        if (chart.difficulty == MusicDifficulty.ReMaster)
            chart.music.id in reMasters
        else
            chart.music.id in musics
    }, record = { record ->
        when {
            plateName.endsWith("極") -> record.comboStatus.isFC()
            plateName.endsWith("将") -> Rate.greaterEqual(record.achievement, "sss")
            plateName.endsWith("神") -> record.comboStatus.isAP()
            plateName.endsWith("舞舞") -> record.syncStatus.isFSD()
            plateName == "覇者" -> record.achievement >= 800000
            else -> throw UnknownError()
        }
    }, name = "plate_$plateName")
    fun version(version: List<GameVersion>) = Filter(chart = { chart ->
        chart.music.genre != MusicGenre.Utage && chart.music.version in version
    })
    fun type(type: MusicType) = Filter(chart = { chart ->
        chart.music.type == type
    })
    val starsNames = listOf("一星", "二星", "三星", "四星", "五星")
    fun stars(stars: Int) = Filter(record = { record ->
        DeluxeScore.stars(record.deluxeScore, record.chart.maxDeluxeScore) == stars
    })
    fun nowVersion(version: GameVersion) = Filter(chart = { chart ->
        chart.music.version.version <= version.version
    }, nowVersion = { version })
    fun tag(musics: List<Int>, tag: String? = null) = Filter(chart = { chart ->
        chart.music.id in musics
    }, name = tag)
    val conditions = buildList {
        designerConfig.aliases.forEach { (designer, aliases) ->
            add(aliases, designer(designer))
        }
        MusicGenre.entries.forEach { genre ->
            add(buildList {
                add(genre.genreName)
                add(genre.value)
                addAll(genre.names)
            }, genre(genre))
        }
        MusicDifficulty.entries.filter { it != MusicDifficulty.Utage }.forEach { difficulty ->
            add(difficulty.names, difficulty(difficulty))
        }
        Level.levelValues.reversed().forEach { levelValue ->
            add(listOf(levelValue.toStringDecimal(1)), levelValue(levelValue))
        }
        Level.levels.reversed().forEach { level ->
            if (Level.numberPart(level) >= 10)
                add(listOf(level + "级", level), level(level))
            else
                add(listOf(level + "级"), level(level))
        }
        add(listOf("寸"), close)
        add(listOf("锁血", "锁", "名刀"), just)
        add(listOf("极", "fc"), fc)
        add(listOf("理论", "ap+", "app"), app)
        add(listOf("神", "ap"), ap)
        add(listOf("fdx+", "fsd+", "fdxp", "fsdp"), fsdp)
        add(listOf("舞舞", "fdx", "fsd"), fsd)
        add(listOf("完整", "全"), noB15)
        add(listOf("拟合定数", "拟合", "nh"), fitLevelValues)
        add(listOf("大将", "鸟加", "sss+", "sssp"), rate("sssp"))
        add(listOf("将", "鸟", "sss"), rate("sss"))
        add(listOf("霸", "clear"), rate("a"))
        add(listOf("牛逼", "nb"), achievement(1008000))
        add(listOf("丢人", "越级"), achievementLess(950000))
        (1..5).forEach { stars ->
            add(listOf(starsNames[stars-1], "${stars}星"), stars(stars))
        }
    }.toMutableList()

    fun allConditions(): List<Pair<List<String>, Filter>> {
        val random = Random(System.currentTimeMillis())
        return conditions + listOf(Pair(listOf("随机"), random(random)))
    }

    fun filters(
        fullCommand: String
    ): List<Filter>? {
        val filters = mutableListOf<Filter>()
        var command = fullCommand
        allConditions().forEach { (names, filter) ->
            names.firstOrNull { name ->
                command.equals(name, true) || command.contains(name, true)
            } ?.let { name ->
                filters.add(filter)
                command = command.replace(name, "", true)
            }
        }
        if (filters.isEmpty())
            return null
        return filters
    }

    fun noRecordFilter(
        filters: List<Filter>?
    ) = filters == null || filters.all { it.record == Filter.defaultRecordFilter }

    fun isDetailed(
        filters: List<Filter>?,
    ): Boolean {
        filters ?: return false
        return filters.any { it.name == "level" }
    }

    fun isPlate(
        filters: List<Filter>?,
    ): Boolean {
        filters ?: return false
        return filters.size == 1 && filters.first().name?.startsWith("plate") == true
    }

    fun filterCharts(
        filters: List<Filter>?,
        musics: Collection<MusicInfo>
    ) = musics.run {
        var charts = this@run.flatMap { it.charts }
        filters?.forEach { filter ->
            charts = charts.filter { chart ->
                filter.chart(chart)
            }
        }
        charts
    }

    fun filterMusics(
        filters: List<Filter>?,
        musics: Collection<MusicInfo>
    ): List<MusicInfo> {
        return filterCharts(filters, musics).map { it.music }.toSet().toList()
    }

    fun filterRecords(
        filters: List<Filter>?,
        records: List<Record>,
        required: Boolean = false
    ): List<Record>? = records.run {
        if (filters == null || required && noRecordFilter(filters))
            return null
        var records = this@run
        filters.forEach { filter ->
            records = records.filter { record ->
                filter.chart(record.chart)
            }.filter { record ->
                filter.record(record)
            }
        }
        records = records.sortedBy(Filter.defaultSort)
        filters.filter { it.sortBy != Filter.defaultSort }.forEach { filter ->
            @Suppress("UNCHECKED_CAST")
            records = records.sortedBy<Record, Comparable<Any>>(filter.sortBy as (Record) -> Comparable<Any>?)
        }
        records
    }

    fun filterTypes(
        filters: List<Filter>?,
    ): RequiresType {
        filters ?: return RequiresType.Achievement
        if (fc in filters || ap in filters)
            return RequiresType.Combo
        if (fsd in filters)
            return RequiresType.Sync
        return filters.mapNotNull { it.name }.firstOrNull { it.startsWith("plate") } ?.let { plateName ->
            when {
                plateName.endsWith("極") -> RequiresType.Combo
                plateName.endsWith("神") -> RequiresType.Combo
                plateName.endsWith("舞舞") -> RequiresType.Sync
                else -> RequiresType.Achievement
            }
        } ?: RequiresType.Achievement
    }

    fun filterNowVersion(
        filters: List<Filter>?,
    ): GameVersion? = filters?.lastOrNull { it.nowVersion != Filter.defaultVersion }?.nowVersion()

    fun isAllRequired(
        filters: List<Filter>?,
    ) = filters ?.any { it.disable15 } ?: false

    fun isFitLevelValues(
        filters: List<Filter>?,
    ) = filters ?.any { it.fitLevelValues } ?: false
}