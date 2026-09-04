package xyz.xszq.bot.chunithm.query

import com.sksamuel.hoplite.ExperimentalHoplite
import korlibs.io.util.toStringDecimal
import xyz.xszq.bot.chunithm.component.ChunithmData
import xyz.xszq.bot.chunithm.component.image.FilterParams
import xyz.xszq.bot.chunithm.music.*
import xyz.xszq.bot.util.toDBC
import kotlin.random.Random

object ComboQuery {
    lateinit var chunithmData: ChunithmData

    val keywordConditions = mutableListOf<Pair<List<String>, Filter>>()
    var sortedKeywordConditions: List<Pair<String, Filter>> = emptyList()
    val regexConditions = mutableListOf<Pair<String, (String) -> Filter>>()

    private val excludeWorldsEnd = Filter(
        FilterType.Default, chart = { it.difficulty != MusicDifficulty.WorldsEnd },
        name = "excludeWorldsEnd"
    )

    fun rules() = register {
        aliases("全连", "fc") { combo(name = "fc") { it.comboStatus.isFC() } }
        aliases("理论", "ajc") { combo(name = "ajc") { it.comboStatus == ComboStatus.AllJusticeCritical } }
        aliases("aj", "ap") { combo(name = "aj") { it.comboStatus.isAJ() } }

        aliases("fullchain") { sync(name = "fullChain") { it.chainStatus.isFullChain() } }

        aliases("寸") {
            achievement(sortBy = { r ->
                when (r.achievement) {
                    in 1004750..1004999 -> 1005000 - r.achievement
                    in 1007250..1007499 -> 1007500 - r.achievement
                    in 1008750..1008999 -> 1009000 - r.achievement
                    else -> 1010000
                }
            }) { r ->
                r.achievement in 1004750..1004999
                        || r.achievement in 1007250..1007499
                        || r.achievement in 1008750..1008999
            }
        }
        aliases("锁血", "锁", "名刀", "血压") {
            achievement(sortBy = { r ->
                when (r.achievement) {
                    in 1005000..1005250 -> r.achievement - 1005000
                    in 1007500..1007750 -> r.achievement - 1007500
                    in 1009000..1009250 -> r.achievement - 1009000
                    else -> 1010000
                }
            }) { r ->
                r.achievement in 1005000..1005250
                        || r.achievement in 1007500..1007750
                        || r.achievement in 1009000..1009250
            }
        }

        aliases("鸟加", "sss+", "sssp") { rateGE("sssp") }
        aliases("纯鸟", "纯sss", "仅鸟", "仅sss") { rate("sss") }
        aliases("鸟", "sss") { rateGE("sss") }
        aliases("通关", "clear") { rateGE("a") }
        aliases("牛逼", "nb") { achievement { it.achievement >= 1009500 } }
        aliases("丢人", "招笑", "越级", "越") { achievement { it.achievement < 950000 } }

        aliases("纯ss+", "仅ss+") { rate("ssp") }
        aliases("纯ss", "仅ss") { rate("ss") }
        aliases("纯s+", "仅s+") { rate("sp") }
        aliases("纯s", "仅s") { rate("s") }
        aliases("纯aaa", "仅aaa") { rate("aaa") }
        aliases("ss+", "ssp") { rateGE("ssp") }
        aliases("ss", "ss") { rateGE("ss") }
        aliases("s+", "sp") { rateGE("sp") }
        aliases("s") { rateGE("s") }
        aliases("aaa") { rateGE("aaa") }

        aliases("完整", "全") { limit(disableN20 = true) }
        aliases("理想") {
            modification(modifier = {
                when (rate) {
                    "sssp" -> {
                        achievement = 1010000
                        comboStatus = ComboStatus.AllJusticeCritical
                    }
                    else -> {
                        rate = Rate.next(rate)
                        achievement = Rate.floor(rate)
                        rating = Rating.calc(chart, achievement)
                    }
                }
            })
        }

        dynamic {
            MusicGenre.entries.forEach { g ->
                aliases(g.genreName, *g.names) { genre(g) }
            }
            MusicDifficulty.entries.filter { it != MusicDifficulty.WorldsEnd }.forEach { d ->
                aliases(*d.names) { difficulty(d) }
            }
            aliases(*MusicDifficulty.WorldsEnd.names) {
                difficulty(MusicDifficulty.WorldsEnd, name = "worldsEnd")
            }
            Level.levelValues.reversed().forEach { value ->
                aliases(value.toStringDecimal(1)) { levelValue(value) }
            }
            Level.levels.reversed().forEach { level ->
                when {
                    Level.numberPart(level) >= 10 -> aliases(listOf("${level}级", level)) { level(level) }
                    else -> aliases(listOf("${level}级")) { level(level) }
                }
            }
            chunithmData.designer.aliases.forEach { (name, aliases) ->
                aliases(aliases) { designer(name) }
            }
        }

        dynamic {
            chunithmData.musics.values.flatMap { music ->
                music.charts.map { chart -> chart.notesDesigner }
            }.toSet().toList().forEach { designer ->
                if (designer.isNotBlank() && designer != "-")
                    add(0, listOf(designer), this@ComboQuery.designer(designer))
            }
        }

        dynamic {
            chunithmData.trophies.values.forEach { trophy ->
                val type = trophy.name.substringBefore(" of ")
                val versionName = trophy.name.substringAfter(" of ")
                val required = trophy.required ?.firstOrNull() ?: return@forEach
                val ids = required.songs ?.map { it.id } ?: return@forEach
                val difficulties = required.difficulties ?.map { MusicDifficulty.of(it) } ?: return@forEach

                add(0, listOf(trophy.name, "$versionName $type"),
                    trophy(
                        songIds = ids,
                        difficulties = difficulties,
                        rank = required.rank,
                        fullCombo = required.fullCombo,
                        fullChain = required.fullChain,
                        name = trophy.name
                    )
                )
                val version = chunithmData.musics[ids.first()] ?.version ?: return@forEach
                add(listOf(versionName, version.name), version(listOf(version)))
            }
        }

        regex("(?<!\\d)(9\\d{5}|100\\d{4}|1010000)(?!\\d)") { matched ->
            achievement { it.achievement == matched.toInt() }
        }
    }

    @OptIn(ExperimentalHoplite::class)
    fun init(data: ChunithmData) {
        chunithmData = data
        rules()
    }

    private fun register(block: ComboQueryBuilder.() -> Unit) {
        val builder = ComboQueryBuilder()
        builder.apply(block)
        keywordConditions += builder.entries
        regexConditions += builder.regexes
        compile()
    }

    fun compile() {
        sortedKeywordConditions = keywordConditions.flatMap { (names, filter) ->
            names.map { name -> name.lowercase() to filter }
        }.sortedByDescending { it.first.length }
    }

    private fun named(name: String) = keywordConditions
        .firstOrNull { (_, filter) -> filter.name == name }
        ?.second

    fun designer(designer: String): Filter {
        val normalized = designer.toDBC()
        val mainName = (chunithmData.designer.aliases.entries.firstOrNull { (key, aliases) ->
            when {
                key.toDBC().equals(normalized, ignoreCase = true) -> true
                aliases.any { it.toDBC().equals(normalized, ignoreCase = true) } -> true
                else -> false
            }
        }?.key ?: designer).toDBC()

        val includesAliases = chunithmData.designer.includes.entries.firstOrNull {
            it.key.toDBC().equals(mainName, ignoreCase = true)
        }?.value ?: emptyList()

        val searchKeywords = (includesAliases + mainName).map { it.toDBC() }.distinct()

        val collabCharts = chunithmData.designer.collabs.entries.firstOrNull {
            when {
                it.key.toDBC().equals(mainName, ignoreCase = true) -> true
                it.key.toDBC().equals(normalized, ignoreCase = true) -> true
                else -> false
            }
        }?.value ?: emptyList()

        return Filter(
            type = FilterType.Designer,
            singleChart = true,
            chart = { chart ->
                val matchAlias = searchKeywords.any { keyword ->
                    chart.notesDesigner.toDBC().contains(keyword, ignoreCase = true)
                }
                val matchCollab = collabCharts.any { raw ->
                    val nowId = raw.substringBefore("#").toInt()
                    val nowDiff = MusicDifficulty.of(raw.substringAfter("#").toInt())
                    chart.music.id == nowId && chart.difficulty == nowDiff
                }
                matchAlias || matchCollab
            }
        )
    }

    fun filters(fullCommand: String): List<Filter>? {
        val filters = mutableListOf<Filter>()
        var command = fullCommand.lowercase()

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
            if (command.contains(name)) {
                if (!filters.contains(filter))
                    filters.add(filter)
                command = command.replace(name, " ")
            }
        }

        if (command.contains("随机")) {
            val rng = Random(System.currentTimeMillis())
            val orders = mutableMapOf<Record, Int>()
            filters.add(Filter(FilterType.Sort, sortBy = { orders.getOrPut(it) { rng.nextInt() } }))
        }
        if (filters.isEmpty())
            return null
        if (filters.none { it.name == "worldsEnd" }) {
            filters.add(0, excludeWorldsEnd)
        }
        return filters
    }

    fun List<Filter>?.filterCharts(musics: Collection<MusicInfo>): List<ChartInfo> {
        if (isNullOrEmpty())
            return musics.flatMap { it.charts }

        val groupedFilters = groupBy { it.type }
        return musics.flatMap { it.charts }.filter { chart ->
            groupedFilters.all { (_, group) ->
                group.any { filter -> filter.chart(chart) }
            }
        }
    }

    fun List<Filter>?.filterMusics(musics: Collection<MusicInfo>): List<MusicInfo> {
        return filterCharts(musics).map { it.music }.toSet().toList()
    }

    fun List<Filter>?.filterRecords(
        records: List<Record>,
        required: Boolean = false
    ): List<Record>? {
        if (this == null || required && noRecordFilter())
            return null

        forEach { filter ->
            records.forEach { record ->
                filter.modifier?.let { record.apply(it) }
            }
        }

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
            filtered = filtered.sortedBy<Record, Comparable<Any>>(
                filter.sortBy as (Record) -> Comparable<Any>?
            )
        }
        return filtered
    }

    fun List<Filter>?.requiresType(): RequiresType {
        this ?: return RequiresType.Achievement
        if (any { it.name in listOf("fc", "aj", "ajc") })
            return RequiresType.Combo
        if (any { it.name in listOf("fullChain") })
            return RequiresType.Sync
        return mapNotNull { it.name }.firstOrNull { it.startsWith("trophy_") }?.let { name ->
            when {
                name.startsWith("trophy_combo_") -> RequiresType.Combo
                name.startsWith("trophy_chain_") -> RequiresType.Sync
                else -> RequiresType.Achievement
            }
        } ?: RequiresType.Achievement
    }

    fun List<Filter>.filterNowVersion(): GameVersion? =
        lastOrNull { it.nowVersion != Filter.defaultVersion }?.nowVersion()

    fun List<Filter>?.noRecordFilter() =
        this == null || all { it.record == Filter.defaultRecordFilter }

    fun List<Filter>?.isDetailed() = when {
        this == null -> false
        else -> any { it.name == "level" }
    }
    fun List<Filter>?.isPlate() = when {
        this == null -> false
        else -> any { it.name?.startsWith("plate") == true }
    }

    fun List<Filter>?.isAllRequired() =
        this?.any { it.disableN20 } ?: false

    fun List<Filter>?.isSingleChartSelected() =
        this?.any { it.singleChart } ?: false

    fun List<Filter>.params(name: String): FilterParams = FilterParams(
        name = name,
        newestVersion = filterNowVersion() ?: chunithmData.newestVersion,
        isAllRequired = isAllRequired(),
        isDetailed = isDetailed(),
        requiresType = requiresType(),
        sortBy = filter { it.sortBy != Filter.defaultSort }.map { it.sortBy }
    )
}