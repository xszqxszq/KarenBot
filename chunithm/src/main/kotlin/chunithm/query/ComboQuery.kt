package xyz.xszq.bot.chunithm.query

import com.sksamuel.hoplite.ExperimentalHoplite
import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.add
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.component.image.FilterParams
import xyz.xszq.bot.chunithm.music.*
import kotlin.math.roundToInt
import kotlin.random.Random


object ComboQuery {
    lateinit var chunithmData: ChunithmData

    val keywordConditions: MutableList<Pair<List<String>, Filter>> = mutableListOf()
    var sortedKeywordConditions: List<Pair<String, Filter>> = emptyList()

    val regexConditions: MutableList<Pair<String, (String) -> Filter>> = mutableListOf()

    val aj = Filter(
        type = FilterType.Combo,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.comboStatus.isAJ()
        }
    )
    val ajc = Filter(
        type = FilterType.Combo,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.comboStatus == ComboStatus.AllJusticeCritical
        }
    )
    val fc = Filter(
        type = FilterType.Combo,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.comboStatus.isFC()
        }
    )
    val fullChain = Filter(
        type = FilterType.Sync,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.chainStatus.isFullChain()
        }
    )
    val fullChainPlatinum = Filter(
        type = FilterType.Sync,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.chainStatus == ChainStatus.Platinum
        }
    )
    val close = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            when (record.achievement) {
                in 1004750..1004999 -> true
                in 1007250..1007499 -> true
                in 1008750..1008999 -> true
                else -> false
            }
        },
        sortBy = { record ->
            when (record.achievement) {
                in 1004750..1004999 -> 1005000 - record.achievement
                in 1007250..1007499 -> 1007500 - record.achievement
                in 1008750..1008999 -> 1009000 - record.achievement
                else -> 1010000
            }
        })
    val just = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            when (record.achievement) {
                in 1005000..1005250 -> true
                in 1007500..1007750 -> true
                in 1009000..1009250 -> true
                else -> false
            }
        },
        sortBy = { record ->
            when (record.achievement) {
                in 1005000..1005250 -> record.achievement - 1005000
                in 1007500..1007750 -> record.achievement - 1007500
                in 1009000..1009250 -> record.achievement - 1009000
                else -> 1010000
            }
        })
    fun rate(rate: String) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.rate == rate
        }
    )
    fun rateGreaterEqual(rate: String) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            Rate.greaterEqual(record.achievement, rate)
        }
    )
    fun achievement(achievement: Int) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        },
        record = { record ->
            record.achievement >= achievement
        }
    )
    fun achievementLess(achievement: Int) = Filter(
        type = FilterType.Achievement,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd
        }, record = { record ->
            record.achievement < achievement
        }
    )
    val noN20 = Filter(
        type = FilterType.Limit,
        disableN20 = true
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
            chart.difficulty != MusicDifficulty.WorldsEnd && chart.levelValue == levelValue
        },
        name = "levelValue",
        singleChart = true
    )
    fun designer(designer: String) = Filter(
        type = FilterType.Designer,
        chart = { chart ->
            (chart.notesDesigner.equals(designer, ignoreCase = true) ||
                    chunithmData.designer.includes[designer]?.let { chart.notesDesigner in it } == true ||
                    chunithmData.designer.collabs[designer]?.let { c ->
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
    fun version(version: List<GameVersion>) = Filter(
        type = FilterType.Version,
        chart = { chart ->
            chart.difficulty != MusicDifficulty.WorldsEnd && chart.music.version in version
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
        val achievement = raw.toInt()
        Filter(
            type = FilterType.Achievement,
            chart = { chart ->
                chart.difficulty != MusicDifficulty.WorldsEnd
            },
            record = { record ->
                record.achievement == achievement
            }
        )
    }

    @OptIn(ExperimentalHoplite::class)
    fun init(data: ChunithmData) {
        chunithmData = data

        keywordConditions.keywords()
        regexConditions.regexes()
        compile()
    }
    fun MutableList<Pair<List<String>, Filter>>.keywords() {
        // 谱师别称
        chunithmData.designer.aliases.forEach { (designer, aliases) ->
            add(aliases, designer(designer))
        }
        // 曲目分类
        MusicGenre.entries.forEach { genre ->
            add(buildList {
                add(genre.genreName)
                addAll(genre.names)
            }, genre(genre))
        }
        // 谱面难度
        MusicDifficulty.entries.filter { it != MusicDifficulty.WorldsEnd }.forEach { difficulty ->
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
        add(listOf("完整", "全"), noN20)
        // FC/FS
        add(listOf("全连", "fc"), fc)
        add(listOf("理论", "ajc"), ajc)
        add(listOf("aj", "ap"), aj)
        add(listOf("fullchain"), fullChain)
        // 成绩分数
        add(listOf("寸"), close)
        add(listOf("锁血", "锁", "名刀", "血压"), just)
        add(listOf("鸟加", "sss+", "sssp"), rateGreaterEqual("sssp"))
        add(listOf("纯鸟", "纯sss", "仅鸟", "仅sss"), rate("sss"))
        add(listOf("鸟", "sss"), rateGreaterEqual("sss"))
        add(listOf("通关", "clear"), rateGreaterEqual("a"))
        add(listOf("牛逼", "nb"), achievement(1009500))
        add(listOf("丢人", "招笑", "越级", "越"), achievementLess(950000))
        // TODO: 版本&牌子
        // 谱师名称
        chunithmData.musics.values.flatMap { music ->
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
    }
    fun MutableList<Pair<String, (String) -> Filter>>.regexes() {
        add("(?<!\\d)(9\\d{5}|100\\d{4}|1010000)(?!\\d)", achievementRegex)
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
        if (fc in this || aj in this)
            return RequiresType.Combo
        if (fullChain in this)
            return RequiresType.Sync
        return RequiresType.Achievement
    }

    fun List<Filter>.filterNowVersion(): GameVersion? =
        lastOrNull { it.nowVersion != Filter.defaultVersion } ?.nowVersion()

    fun List<Filter>?.noRecordFilter() =
        this == null || all { it.record == Filter.defaultRecordFilter }

    fun List<Filter>?.isDetailed() = when {
        this == null -> false
        else -> any { it.name == "level" }
    }

    fun List<Filter>?.isAllRequired() =
        this ?.any { it.disableN20 } ?: false

    fun List<Filter>?.isSingleChartSelected() =
        this ?.any { it.singleChart } ?: false

    fun List<Filter>.params(
        name: String
    ): FilterParams = FilterParams(
        name = name,
        newestVersion = filterNowVersion() ?: chunithmData.newestVersion,
        isAllRequired = isAllRequired(),
        isDetailed = isDetailed(),
        requiresType = requiresType(),
        sortBy = filter { it.sortBy != Filter.defaultSort }.map { it.sortBy }
    )
}
