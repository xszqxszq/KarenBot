package xyz.xszq.bot.maimai.query

import xyz.xszq.bot.maimai.music.*
import kotlin.random.Random

@ComboQueryDsl
@Suppress("unused")
class ComboQueryBuilder {
    val entries = mutableListOf<Pair<List<String>, Filter>>()
    val regexes = mutableListOf<Pair<String, (String) -> Filter>>()

    fun aliases(vararg names: String, block: ComboQueryBuilder.() -> Filter) {
        entries += names.toList() to block()
    }

    fun aliases(names: List<String>, block: ComboQueryBuilder.() -> Filter) {
        entries += names to block()
    }

    fun regex(pattern: String, block: (String) -> Filter) {
        regexes += pattern to block
    }

    fun dynamic(block: ComboQueryBuilder.() -> Unit) {
        block()
    }

    fun add(aliases: List<String>, filter: Filter) {
        entries += aliases to filter
    }

    fun add(index: Int, aliases: List<String>, filter: Filter) {
        entries.add(index, aliases to filter)
    }

    fun combo(
        name: String? = null,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        block: (Record) -> Boolean = { true }
    ) = Filter(FilterType.Combo, record = block, sortBy = sortBy, name = name)

    fun sync(
        name: String? = null,
        block: (Record) -> Boolean = { true }
    ) = Filter(FilterType.Sync, record = block, name = name)

    fun achievement(
        name: String? = null,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        block: (Record) -> Boolean = { true }
    ) = Filter(FilterType.Achievement, record = block, sortBy = sortBy, name = name)

    fun modification(
        name: String? = null,
        fitLevelValue: Boolean = false,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        modifier: (Record.() -> Unit)? = null
    ) = Filter(FilterType.Modification, sortBy = sortBy, name = name,
        fitLevelValue = fitLevelValue, modifier = modifier)

    fun limit(disable15: Boolean = false) =
        Filter(FilterType.Limit, disable15 = disable15)

    fun difficulty(d: MusicDifficulty, name: String? = null) =
        Filter(FilterType.Difficulty, chart = { it.difficulty == d },
            singleChart = true, name = name)

    fun genre(g: MusicGenre) =
        Filter(FilterType.Genre, chart = { it.music.genre == g })

    fun level(l: String) =
        Filter(FilterType.Level, chart = { it.level == l },
            name = "level", singleChart = true)

    fun levelValue(v: Double) =
        Filter(FilterType.Level, chart = { it.levelValue == v },
            name = "levelValue", singleChart = true)

    fun stars(n: Int) = Filter(FilterType.Star, record = {
        DeluxeScore.stars(it.deluxeScore, it.chart.maxDeluxeScore) == n
    })

    fun type(t: MusicType) =
        Filter(FilterType.Type, chart = { it.music.type == t })

    fun version(v: List<GameVersion>) =
        Filter(FilterType.Version, chart = { it.music.version in v })

    fun tag(musics: List<Int>, name: String? = null) =
        Filter(FilterType.Tag, chart = { it.music.id in musics }, name = name)

    fun rate(r: String) =
        achievement { it.rate == r }

    fun rateGE(r: String) =
        achievement { Rate.greaterEqual(it.achievement, r) }

    fun nowVersion(v: GameVersion) =
        Filter(FilterType.Version, chart = { it.music.version.version <= v.version },
            nowVersion = { v })

    fun random(random: Random): Filter {
        val orders = mutableMapOf<Record, Int>()
        return Filter(FilterType.Sort, sortBy = { orders.getOrPut(it) { random.nextInt() } })
    }

    fun plate(musics: List<Int>, reMasters: List<Int>, name: String) =
        Filter(FilterType.Plate, chart = { chart ->
            if (chart.difficulty == MusicDifficulty.ReMaster)
                chart.music.id in reMasters
            else chart.music.id in musics
        }, record = { record ->
            when {
                name.endsWith("極") -> record.comboStatus.isFC()
                name.endsWith("将") -> Rate.greaterEqual(record.achievement, "sss")
                name.endsWith("神") -> record.comboStatus.isAP()
                name.endsWith("舞舞") -> record.syncStatus.isFSD()
                name == "覇者" -> record.achievement >= 800000
                else -> throw UnknownError()
            }
        }, name = "plate_$name")
}
