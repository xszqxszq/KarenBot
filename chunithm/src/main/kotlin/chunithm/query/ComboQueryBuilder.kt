package xyz.xszq.bot.chunithm.query

import xyz.xszq.bot.chunithm.music.*

@ComboQueryDsl
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
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        modifier: (Record.() -> Unit)? = null
    ) = Filter(FilterType.Modification, sortBy = sortBy, name = name, modifier = modifier)

    fun limit(disableN20: Boolean = false) =
        Filter(FilterType.Limit, disableN20 = disableN20)

    fun difficulty(d: MusicDifficulty, name: String? = null) =
        Filter(FilterType.Difficulty, chart = { it.difficulty == d }, singleChart = true, name = name)

    fun genre(g: MusicGenre) =
        Filter(FilterType.Genre, chart = { it.music.genre == g })

    fun level(l: String) =
        Filter(FilterType.Level, chart = { it.level == l }, name = "level", singleChart = true)

    fun levelValue(v: Double) =
        Filter(FilterType.Level, chart = { it.levelValue == v }, name = "levelValue", singleChart = true)

    fun version(v: List<GameVersion>) =
        Filter(FilterType.Version, chart = { it.music.version in v })

    fun rate(r: String) =
        achievement { it.rate == r }

    fun rateGE(r: String) =
        achievement { Rate.greaterEqual(it.achievement, r) }

    fun nowVersion(v: GameVersion) =
        Filter(FilterType.Modification, chart = { it.music.version.version <= v.version }, nowVersion = { v })

    fun trophy(
        songIds: List<Int>,
        difficulties: List<MusicDifficulty>,
        rank: String ?= null,
        fullCombo: String ?= null,
        fullChain: String ?= null,
        name: String
    ) = Filter(FilterType.Trophy, chart = { chart ->
        chart.music.id in songIds && chart.difficulty in difficulties
    }, record = { record ->
        when {
            rank != null -> Rate.greaterEqual(record.achievement, rank)
            fullCombo == "alljustice" -> record.comboStatus.isAJ()
            fullCombo == "fullcombo" -> record.comboStatus.isFC()
            fullChain == "fullchain" -> record.chainStatus.isFullChain()
            else -> true
        }
    }, name = "trophy_" + when {
        fullCombo != null -> "combo_"
        fullChain != null -> "chain_"
        else -> ""
    } + name)
}
