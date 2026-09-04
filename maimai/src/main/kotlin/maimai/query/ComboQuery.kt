package xyz.xszq.bot.maimai.query

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import korlibs.io.util.toStringDecimal
import korlibs.math.toIntRound
import xyz.xszq.bot.maimai.component.MaimaiData
import xyz.xszq.bot.maimai.component.Tag
import xyz.xszq.bot.maimai.component.image.FilterParams
import xyz.xszq.bot.maimai.config.DesignerConfig
import xyz.xszq.bot.maimai.music.*
import xyz.xszq.bot.maimai.toSimple
import xyz.xszq.bot.util.json
import xyz.xszq.bot.util.toDBC
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random

object ComboQuery {
    lateinit var designerConfig: DesignerConfig
    lateinit var maimaiData: MaimaiData

    val keywordConditions = mutableListOf<Pair<List<String>, Filter>>()
    var sortedKeywordConditions: List<Pair<String, Filter>> = emptyList()
    val regexConditions = mutableListOf<Pair<String, (String) -> Filter>>()

    private val excludeUtage = Filter(
        FilterType.Default, chart = { it.difficulty != MusicDifficulty.Utage },
        name = "excludeUtage"
    )

    fun rules() = register {
        aliases("极", "全连", "fc") {
            combo(name = "fc") { it.comboStatus.isFC() }
        }
        aliases("理论", "ap+", "app") {
            combo(name = "app") { it.comboStatus == ComboStatus.AllPerfectPlus }
        }
        aliases("神", "ap") {
            combo(name = "ap") { it.comboStatus.isAP() }
        }

        aliases("fdx+", "fsd+", "fdxp", "fsdp") {
            sync(name = "fsdp") { it.syncStatus == SyncStatus.FullSyncDeluxePlus }
        }
        aliases("舞舞", "fdx", "fsd") {
            sync(name = "fsd") { it.syncStatus.isFSD() }
        }

        aliases("寸") {
            achievement(sortBy = { r ->
                var target = (r.achievement.toDouble() / 10000).toIntRound() * 10000
                if (target < r.achievement) target += 5000
                target - r.achievement
            }) { r ->
                val decimal = r.achievement % 10000
                r.achievement in 994250..1004999
                        && (decimal in 4250..4999 || decimal in 9250..9999)
            }
        }
        aliases("锁血", "锁", "名刀", "血压") {
            achievement(sortBy = { r ->
                var target = (r.achievement.toDouble() / 10000).toIntRound() * 10000
                if (target > r.achievement) target -= 5000
                r.achievement - target
            }) { r ->
                val decimal = r.achievement % 10000
                r.achievement in 1000000..1005250
                        && (decimal in 5000..5250 || decimal in 0..1250)
            }
        }

        aliases("大将", "鸟加", "sss+", "sssp") { rateGE("sssp") }
        aliases("将") { rateGE("sss") }
        aliases("纯鸟", "纯sss", "仅鸟", "仅sss") { rate("sss") }
        aliases("鸟", "sss") { rateGE("sss") }
        aliases("霸", "clear") { rateGE("a") }
        aliases("牛逼", "nb") { achievement { it.achievement >= 1008000 } }
        aliases("丢人", "招笑", "越级", "越") { achievement { it.achievement < 950000 } }

        aliases("纯ss+", "仅ss+") { rate("ssp") }
        aliases("纯ss", "仅ss") { rate("ss") }
        aliases("纯s+", "仅s+") { rate("sp") }
        aliases("纯s", "仅s") { rate("s") }
        aliases("纯aaa", "仅aaa") { rate("aaa") }
        aliases("ss+", "ssp") { rateGE("ssp") }
        aliases("ss") { rateGE("ss") }
        aliases("s+", "sp") { rateGE("sp") }
        aliases("s") { rateGE("s") }
        aliases("aaa") { rateGE("aaa") }

        aliases("完整", "全") { limit(disable15 = true) }
        aliases("拟合定数", "拟合", "nh") {
            modification(
                fitLevelValue = true,
                sortBy = { -Rating.calc(it.chart.fitLevelValue, it.achievement) }
            )
        }
        aliases("理想") {
            modification(modifier = {
                when (rate) {
                    "sssp" -> {
                        achievement = 1010000
                        comboStatus = ComboStatus.AllPerfectPlus
                    }
                    else -> {
                        rate = Rate.next(rate)
                        achievement = Rate.floor(rate)
                        rating = Rating.calc(chart, achievement)
                    }
                }
            })
        }

        aliases("宴谱", "宴会场") { difficulty(MusicDifficulty.Utage, name = "utage") }
        aliases("标准", "标") { type(MusicType.Standard) }
        aliases("dx谱") { type(MusicType.Deluxe) }
        aliases("旧框") {
            version(maimaiData.versions.values.filter { it.version <= 19900 })
        }
        aliases("dx") {
            version(maimaiData.versions.values.filter { it.version > 19900 })
        }
        aliases("旧版本", "旧") {
            version(maimaiData.versions.values.filter { it != maimaiData.newestVersion })
        }
        aliases("新版本", "新歌", "新") {
            version(listOf(maimaiData.newestVersion))
        }

        dynamic {
            listOf("一星", "二星", "三星", "四星", "五星").forEachIndexed { index, name ->
                aliases(name, "${index + 1}星") { stars(index + 1) }
            }
            MusicGenre.entries.filter { it != MusicGenre.Utage }.forEach { g ->
                aliases(g.genreName, g.value, *g.names) { genre(g) }
            }
            MusicDifficulty.entries.filter { it != MusicDifficulty.Utage }.forEach { d ->
                aliases(*d.names) { difficulty(d) }
            }
            Level.levelValues.reversed().forEach { value ->
                aliases(value.toStringDecimal(1)) { levelValue(value) }
            }
            Level.levels.reversed().forEach { level ->
                when {
                    Level.numberPart(level) >= 10 -> aliases(listOf("${level}级", level)) { level(level) }
                    else -> aliases(listOf("${level}级")) { level(level)}
                }
            }
            designerConfig.aliases.forEach { (name, aliases) ->
                aliases(aliases) { designer(name) }
            }
        }

        dynamic {
            maimaiData.plates.values.filter {
                it.genre == "実績" && it.requires.isNotEmpty() && it.name != "覇者"
            }.associateBy {
                it.name.replace(Item.plateTypes.first { type -> it.name.endsWith(type) }, "")
            }.also { filtered ->
                val early = filtered.filter { (version, _) ->
                    version in listOf("真", "超", "檄")
                }.flatMap { (_, plate) ->
                    plate.requires.mapNotNull { maimaiData.musics[it]?.version }.toSet().toList()
                }.toSet().toList()
                add(0, listOf("真超檄"), version(early))
            }.forEach { (version, plate) ->
                val simplified = Item.simplifyTable[version] ?: version.toSimple()
                val gameVersions = plate.requires
                    .mapNotNull { maimaiData.musics[it]?.version }.toSet().toList()
                add(listOf(version, simplified), version(gameVersions))
                add(0, listOf(simplified + "代"), version(gameVersions))
            }
        }

        dynamic {
            maimaiData.versions.values.filter { it.version > 20000 }.forEach { v ->
                val year = v.name.substringAfter("舞萌DX ")
                add(0, listOf("舞萌dx$year", "dx$year", year), nowVersion(v))
            }
            maimaiData.versions.values.firstOrNull { it.version == 20000 }?.let { v ->
                add(0, listOf("dx无印"), nowVersion(v))
            }
        }

        dynamic {
            maimaiData.musics.values.flatMap { music ->
                music.charts.map { chart -> chart.notesDesigner }
            }.toSet().toList().forEach { designer ->
                if (designer.isNotBlank() && designer != "-")
                    add(0, listOf(designer), this@ComboQuery.designer(designer))
            }
        }

        dynamic {
            maimaiData.plates.values.filter {
                it.genre == "実績" && it.requires.isNotEmpty()
            }.forEach { p ->
                val name = Item.toSimplified(p.name)
                add(0, listOf(p.name, name), plate(p.requires, p.remasters, p.name))
            }
        }

        dynamic {
            val customTags = json.decodeFromString<Map<String, Tag>>(
                File(maimaiData.dataDir.absolutePath + "/tag.json").readText(Charsets.UTF_8)
            )
            customTags.forEach { (_, tag) ->
                aliases(tag.aliases) { tag(tag.musics, tag.name) }
            }
        }

        regex("(?<!\\d)(?:10[0-1]|[1-9]?\\d)\\.\\d{1,4}(?=%|％)") { matched ->
            achievement { it.achievement == (matched.toDouble() * 10000).roundToInt() }
        }
    }

    @OptIn(ExperimentalHoplite::class)
    fun init(data: MaimaiData) {
        designerConfig = ConfigLoaderBuilder.default()
            .addFileSource("${data.dataPath}/designer.yml")
            .withExplicitSealedTypes()
            .build()
            .loadConfigOrThrow<DesignerConfig>()
        maimaiData = data

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
        val mainName = (designerConfig.aliases.entries.firstOrNull { (key, aliases) ->
            when {
                key.toDBC().equals(normalized, ignoreCase = true) -> true
                aliases.any { it.toDBC().equals(normalized, ignoreCase = true) } -> true
                else -> false
            }
        }?.key ?: designer).toDBC()

        val includesAliases = designerConfig.includes.entries.firstOrNull {
            it.key.toDBC().equals(mainName, ignoreCase = true)
        }?.value ?: emptyList()

        val searchKeywords = (includesAliases + mainName).map { it.toDBC() }.distinct()

        val collabCharts = designerConfig.collabs.entries.firstOrNull {
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
        if (filters.none { it.name == "utage" }) {
            filters.add(0, excludeUtage)
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
        if (any { it.name in listOf("fc", "ap", "app") })
            return RequiresType.Combo
        if (any { it.name in listOf("fsd", "fsdp") })
            return RequiresType.Sync
        return mapNotNull { it.name }.firstOrNull { it.startsWith("plate") }?.let { plateName ->
            when {
                plateName.endsWith("極") -> RequiresType.Combo
                plateName.endsWith("神") -> RequiresType.Combo
                plateName.endsWith("舞舞") -> RequiresType.Sync
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
        this?.any { it.disable15 } ?: false

    fun List<Filter>?.isFitLevelValue() =
        this?.any { it.fitLevelValue } ?: false

    fun List<Filter>?.isSingleChartSelected() =
        this?.any { it.singleChart } ?: false

    fun List<Filter>.params(name: String): FilterParams = FilterParams(
        name = name,
        newestVersion = filterNowVersion() ?: maimaiData.newestVersion,
        isAllRequired = isAllRequired(),
        isFitLevelValue = isFitLevelValue(),
        isDetailed = isDetailed(),
        requiresType = requiresType(),
        sortBy = filter { it.sortBy != Filter.defaultSort }.map { it.sortBy }
    )
}