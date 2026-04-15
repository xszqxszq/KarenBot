package xyz.xszq.bot.maimai.query

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.util.toStringDecimal
import korlibs.math.toIntRound
import xyz.xszq.bot.add
import xyz.xszq.bot.json
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.component.Tag
import xyz.xszq.bot.maimai.component.image.FilterParams
import xyz.xszq.bot.maimai.config.DesignerConfig
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.toSimple
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random

object ComboQuery {
    lateinit var designerConfig: DesignerConfig
    lateinit var maimaiData: MaimaiData

    val keywordConditions: MutableList<Pair<List<String>, Filter>> = mutableListOf()
    var sortedKeywordConditions: List<Pair<String, Filter>> = emptyList()

    val regexConditions: MutableList<Pair<String, (String) -> Filter>> = mutableListOf()

    val ap = Filter(
        type = FilterType.Combo,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.comboStatus.isAP()
        }
    )
    val app = Filter(
        type = FilterType.Combo,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.comboStatus == ComboStatus.AllPerfectPlus
        }
    )
    val fc = Filter(
        type = FilterType.Combo,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.comboStatus.isFC()
        }
    )
    val fsd = Filter(
        type = FilterType.Sync,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.syncStatus.isFSD()
        }
    )
    val fsdp = Filter(
        type = FilterType.Sync,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.syncStatus == SyncStatus.FullSyncDeluxePlus
        }
    )
    val close = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            val decimal = record.achievement % 10000
            record.achievement in 994250..1004999
                    && (decimal in 4250..4999 || decimal in 9250..9999)
        },
        sortBy = { record ->
            var target = (record.achievement.toDouble() / 10000).toIntRound() * 10000
            if (target < record.achievement)
                target += 5000
            target - record.achievement
        })
    val just = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            val decimal = record.achievement % 10000
            record.achievement in 1000000..1005250
                    && (decimal in 5000..5250 || decimal in 0..1250)
        },
        sortBy = { record ->
            var target = (record.achievement.toDouble() / 10000).toIntRound() * 10000
            if (target > record.achievement)
                target -= 5000
            record.achievement - target
        })
    fun rate(rate: String) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.rate == rate
        }
    )
    fun rateGreaterEqual(rate: String) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            Rate.greaterEqual(record.achievement, rate)
        }
    )
    fun achievement(achievement: Int) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        },
        record = { record ->
            record.achievement >= achievement
        }
    )
    fun achievementLess(achievement: Int) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage
        }, record = { record ->
            record.achievement < achievement
        }
    )
    val noB15 = Filter(
        type = FilterType.Limit,
        disable15 = true
    )
    fun random(random: Random) = Filter(
        type = FilterType.Sort,
        sortBy = { record ->
            random.nextInt()
        }
    )
    fun difficulty(difficulty: MusicDifficulty) = Filter(
        type = FilterType.Difficulty,
        chart = { chart ->
            chart.difficulty == difficulty
        },
        singleChart = true
    )
    fun genre(genre: MusicGenre) = Filter(
        type = FilterType.Genre,
        chart = { chart ->
            chart.music.genre == genre
        }
    )
    fun tag(musics: List<Int>, tag: String? = null) = Filter(
        type = FilterType.Tag,
        chart = { chart ->
            chart.music.id in musics
        }, name = tag)
    fun level(level: String) = Filter(
        type = FilterType.Level,
        chart = { chart ->
            chart.level == level
        },
        name = "level",
        singleChart = true
    )
    fun levelValue(levelValue: Double) = Filter(
        type = FilterType.Level,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.Utage && chart.levelValue == levelValue
        },
        name = "levelValue",
        singleChart = true
    )
    fun designer(designer: String) = Filter(
        type = FilterType.Designer,
        chart = { chart ->
            (chart.notesDesigner.equals(designer, ignoreCase = true) ||
                    designerConfig.includes[designer]?.let { chart.notesDesigner in it } == true ||
                    designerConfig.collabs[designer]?.let { c ->
                        c.any { raw ->
                            val nowId = raw.substringBefore("#").toInt()
                            val nowDiff = MusicDifficulty.of(raw.substringAfter("#").toInt())
                            chart.music.id == nowId && chart.difficulty == nowDiff
                        }
                    } == true ||
                    designer.lowercase() in chart.notesDesigner.lowercase())
        },
        singleChart = true
    )
    fun musicsPlate(musics: List<Int>, reMasters: List<Int>, plateName: String) = Filter(
        type = FilterType.Plate,
        chart = { chart ->
            if (chart.difficulty == MusicDifficulty.ReMaster)
                chart.music.id in reMasters
            else
                chart.music.id in musics
        },
        record = { record ->
            when {
                plateName.endsWith("極") -> record.comboStatus.isFC()
                plateName.endsWith("将") -> Rate.greaterEqual(record.achievement, "sss")
                plateName.endsWith("神") -> record.comboStatus.isAP()
                plateName.endsWith("舞舞") -> record.syncStatus.isFSD()
                plateName == "覇者" -> record.achievement >= 800000
                else -> throw UnknownError()
            }
        }, name = "plate_$plateName"
    )
    fun version(version: List<GameVersion>) = Filter(
        type = FilterType.Version,
        chart = { chart ->
            chart.music.genre != MusicGenre.Utage && chart.music.version in version
        }
    )
    fun type(type: MusicType) = Filter(
        type = FilterType.Type,
        chart = { chart ->
            chart.music.type == type
        }
    )
    val starsNames = listOf("一星", "二星", "三星", "四星", "五星")
    fun stars(stars: Int) = Filter(
        type = FilterType.Star,
        record = { record ->
            DeluxeScore.stars(record.deluxeScore, record.chart.maxDeluxeScore) == stars
        }
    )
    val fitLevelValues = Filter(
        type = FilterType.Modification,
        fitLevelValue = true,
        sortBy = { record ->
            -Rating.calc(record.chart.fitLevelValue, record.achievement)
        }
    )
    fun nowVersion(version: GameVersion) = Filter(
        type = FilterType.Modification,
        chart = { chart ->
            chart.music.version.version <= version.version
        },
        nowVersion = { version }
    )
    val achievementRegex: (String) -> Filter = { raw ->
        val achievement = (raw.toDouble() * 10000).roundToInt()
        Filter(
            type = FilterType.Achievement,
            chart = { chart ->
                chart.difficulty != MusicDifficulty.Utage
            },
            record = { record ->
                record.achievement == achievement
            }
        )
    }

    @OptIn(ExperimentalHoplite::class)
    fun init(data: MaimaiData) {
        designerConfig = ConfigLoaderBuilder.default()
            .addFileSource("./data/maimai/designer.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<DesignerConfig>()
        maimaiData = data

        keywordConditions.addConditions()
        regexConditions.regexes()
        compile()
    }
    fun MutableList<Pair<List<String>, Filter>>.addConditions() {
        // 谱师别称
        designerConfig.aliases.forEach { (designer, aliases) ->
            add(aliases, designer(designer))
        }
        // 曲目分类
        MusicGenre.entries.forEach { genre ->
            add(buildList {
                add(genre.genreName)
                add(genre.value)
                addAll(genre.names)
            }, genre(genre))
        }
        // 谱面难度
        MusicDifficulty.entries.filter { it != MusicDifficulty.Utage }.forEach { difficulty ->
            add(difficulty.names, difficulty(difficulty))
        }
        // 谱面定数
        Level.levelValues.reversed().forEach { levelValue ->
            add(listOf(levelValue.toStringDecimal(1)), levelValue(levelValue))
        }
        // 谱面等级
        Level.levels.reversed().forEach { level ->
            if (Level.numberPart(level) >= 10)
                add(listOf(level + "级", level), level(level))
            else
                add(listOf(level + "级"), level(level))
        }
        // 特殊条件
        add(listOf("完整", "全"), noB15)
        add(listOf("拟合定数", "拟合", "nh"), fitLevelValues)
        // FC/FS
        add(listOf("极", "全连", "fc"), fc)
        add(listOf("理论", "ap+", "app"), app)
        add(listOf("神", "ap"), ap)
        add(listOf("fdx+", "fsd+", "fdxp", "fsdp"), fsdp)
        add(listOf("舞舞", "fdx", "fsd"), fsd)
        // 成绩分数
        add(listOf("寸"), close)
        add(listOf("锁血", "锁", "名刀", "血压"), just)
        add(listOf("大将", "鸟加", "sss+", "sssp"), rateGreaterEqual("sssp"))
        add(listOf("将"), rateGreaterEqual("sss"))
        add(listOf("纯鸟", "纯sss", "仅鸟", "仅sss"), rate("sss"))
        add(listOf("鸟", "sss"), rateGreaterEqual("sss"))
        add(listOf("霸", "clear"), rateGreaterEqual("a"))
        add(listOf("牛逼", "nb"), achievement(1008000))
        add(listOf("丢人", "越级", "越"), achievementLess(950000))
        (1..5).forEach { stars ->
            add(listOf(starsNames[stars-1], "${stars}星"), stars(stars))
        }
        // 标准版本
        maimaiData.plates.values.filter {
            it.genre == "実績" && it.requires.isNotEmpty() && it.name != "覇者"
        }.associateBy {
            it.name.replace(Item.plateTypes.first { type -> it.name.endsWith(type) }, "")
        }.also { filtered ->
            val early = filtered.filter { (version, _) ->
                version in listOf("真", "超", "檄")
            }.flatMap { (_, plate) ->
                plate.requires.mapNotNull { maimaiData.musics[it] ?.version }.toSet().toList()
            }.toSet().toList()
            add(0, Pair(listOf("真超檄"),version(early)))
        }.forEach { (version, plate) ->
            val simplified = Item.simplifyTable[version] ?: version.toSimple()
            val gameVersions = plate.requires.mapNotNull { maimaiData.musics[it] ?.version }.toSet().toList()
            add(Pair(listOf(version, simplified),
                version(gameVersions)))
            add(0, Pair(listOf(simplified + "代"),
                version(gameVersions)))
        }
        // DX版本
        maimaiData.versions.values.filter { it.version > 20000 }.forEach { version ->
            val year = version.name.substringAfter("舞萌DX ")
            add(0, Pair(listOf("dx$year", year), nowVersion(version)))
        }
        maimaiData.versions.values.firstOrNull { it.version == 20000 } ?.let { version ->
            add(0, Pair(listOf("dx无印"), nowVersion(version)))
        }
        // 谱面类型
        add(Pair(listOf("标准", "标"), type(MusicType.Standard)))
        add(Pair(listOf("dx谱"), type(MusicType.Deluxe)))
        add(Pair(listOf("旧框"),
            version(maimaiData.versions.values.filter { it.version <= 19900 })
        ))
        add(Pair(listOf("dx"),
            version(maimaiData.versions.values.filter { it.version > 19900 })
        ))
        add(Pair(listOf("旧版本", "旧"),
            version(maimaiData.versions.values.filter { it != maimaiData.newestVersion })
        ))
        add(Pair(listOf("新版本", "新歌", "新"),
            version(listOf(maimaiData.newestVersion))
        ))
        // 牌子谱面
        maimaiData.plates.values.filter {
            it.genre == "実績" && it.requires.isNotEmpty()
        }.forEach { plate ->
            val name = Item.toSimplified(plate.name)
            add(0, Pair(listOf(plate.name, name),
                musicsPlate(plate.requires, plate.remasters, plate.name)))
        }
        // 谱师名称
        maimaiData.musics.values.flatMap { music ->
            music.charts.map { chart -> chart.notesDesigner }
        }.toSet().toList().forEach { designer ->
            if (designer.isNotBlank() && designer != "-")
                add(0, Pair(listOf(designer), designer(designer)))
        }
        // 谱面分数
        add(listOf("纯ss+", "仅ss+"), rate("ssp"))
        add(listOf("纯ss", "仅ss"), rate("ss"))
        add(listOf("纯s+", "仅s+"), rate("sp"))
        add(listOf("纯s", "仅s"), rate("s"))
        add(listOf("纯aaa", "仅aaa"), rate("aaa"))
        add(listOf("ss+", "ssp"), rateGreaterEqual("ssp"))
        add(listOf("ss", "ss"), rateGreaterEqual("ss"))
        add(listOf("s+", "sp"), rateGreaterEqual("sp"))
        add(listOf("s"), rateGreaterEqual("s"))
        add(listOf("aaa"), rateGreaterEqual("aaa"))
        // 谱面标签
        val customTags = json.decodeFromString<Map<String, Tag>>(
            File(maimaiData.dataDir.absolutePath + "/tag.json").readText(Charsets.UTF_8)
        )
        customTags.forEach { (_, tag) ->
            add(Pair(tag.aliases, tag(tag.musics, tag.name)))
        }
    }
    fun MutableList<Pair<String, (String) -> Filter>>.regexes() {
        add("(?<!\\d)(?:10[0-1]|[1-9]?\\d)\\.\\d{1,4}(?=%|％)", achievementRegex)
    }

    fun compile() {
        sortedKeywordConditions = keywordConditions.flatMap { (names, filter) ->
            names.map { name -> name to filter }
        }.sortedByDescending { it.first.length }
    }

    fun filters(
        fullCommand: String
    ): List<Filter>? {
        val filters = mutableListOf<Filter>()
        var command = fullCommand

        regexConditions.forEach { (pattern, getFilter) ->
            val regex = Regex(pattern)
            regex.findAll(command).forEach { match ->
                val filter = getFilter(match.value)
                if (!filters.contains(filter)) {
                    filters.add(filter)
                }
            }
            command = regex.replace(command, " ")
        }

        sortedKeywordConditions.forEach { (name, filter) ->
            if (command.contains(name, ignoreCase = true)) {
                if (!filters.contains(filter))
                    filters.add(filter)
                command = command.replace(name, " ", ignoreCase = true)
            }
        }

        if (command.contains("随机")) {
            val random = Random(System.currentTimeMillis())
            filters.add(random(random))
        }
        if (filters.isEmpty())
            return null
        return filters
    }

    fun List<Filter>?.filterCharts(
        musics: Collection<MusicInfo>
    ): List<ChartInfo> {
        if (isNullOrEmpty())
            return musics.flatMap { it.charts }

        val groupedFilters = groupBy { it.type }
        return musics.flatMap {
            it.charts
        }.filter { chart ->
            groupedFilters.all { (_, group) ->
                group.any { filter ->
                    filter.chart(chart)
                }
            }
        }
    }

    fun List<Filter>?.filterMusics(
        musics: Collection<MusicInfo>
    ): List<MusicInfo> {
        return filterCharts(musics).map { it.music }.toSet().toList()
    }

    fun List<Filter>?.filterRecords(
        records: List<Record>,
        required: Boolean = false
    ): List<Record>? {
        if (this == null || required && noRecordFilter())
            return null

        val groupedFilters = groupBy { it.type }
        var filtered = records.filter { record ->
            groupedFilters.all { (_, group) ->
                group.any { filter ->
                    filter.chart(record.chart) && filter.record(record)
                }
            }
        }
        filtered = filtered.sortedBy(Filter.defaultSort)
        filter { it.sortBy != Filter.defaultSort }.forEach { filter ->
            @Suppress("UNCHECKED_CAST")
            filtered = filtered.sortedBy<Record, Comparable<Any>>(filter.sortBy as (Record) -> Comparable<Any>?)
        }
        return filtered
    }

    fun List<Filter>?.requiresType(): RequiresType {
        this ?: return RequiresType.Achievement
        if (fc in this || ap in this)
            return RequiresType.Combo
        if (fsd in this)
            return RequiresType.Sync
        return mapNotNull { it.name }.firstOrNull { it.startsWith("plate") } ?.let { plateName ->
            when {
                plateName.endsWith("極") -> RequiresType.Combo
                plateName.endsWith("神") -> RequiresType.Combo
                plateName.endsWith("舞舞") -> RequiresType.Sync
                else -> RequiresType.Achievement
            }
        } ?: RequiresType.Achievement
    }

    fun List<Filter>.filterNowVersion(): GameVersion? =
        lastOrNull { it.nowVersion != Filter.defaultVersion } ?.nowVersion()

    fun List<Filter>?.noRecordFilter() =
        this == null || all { it.record == Filter.defaultRecordFilter }

    fun List<Filter>?.isDetailed() = when {
        this == null -> false
        else -> any { it.name == "level" }
    }

    fun List<Filter>?.isPlate() = when {
        this == null -> false
        else -> size == 1 && first().name?.startsWith("plate") == true
    }

    fun List<Filter>?.isAllRequired() =
        this ?.any { it.disable15 } ?: false

    fun List<Filter>?.isFitLevelValue() =
        this ?.any { it.fitLevelValue } ?: false

    fun List<Filter>?.isSingleChartSelected() =
        this ?.any { it.singleChart } ?: false

    fun List<Filter>.params(
        name: String
    ): FilterParams = FilterParams(
        name = name,
        newestVersion = filterNowVersion() ?: maimaiData.newestVersion,
        isAllRequired = isAllRequired(),
        isFitLevelValue = isFitLevelValue(),
        isDetailed = isDetailed(),
        requiresType = requiresType(),
        sortBy = filter { it.sortBy != Filter.defaultSort }.map { it.sortBy }
    )
}
