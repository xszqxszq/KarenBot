package xyz.xszq.bot.chunithm.query

import xyz.xszq.bot.chunithm.music.*

/**
 * 随心配查询的 DSL Builder
 */
@ComboQueryDsl
@Suppress("unused")
class ComboQueryBuilder {
    val entries = mutableListOf<Pair<List<String>, Filter>>()
    val regexes = mutableListOf<Pair<String, (String) -> Filter>>()

    /**
     * 添加一组别名与条件
     *
     * @param names 别名
     * @param block 条件的 DSL Builder
     */
    fun aliases(vararg names: String, block: ComboQueryBuilder.() -> Filter) {
        entries += names.toList() to block()
    }

    /**
     * 添加一组别名与条件
     *
     * @param names 别名
     * @param block 条件的 DSL Builder
     */
    fun aliases(names: List<String>, block: ComboQueryBuilder.() -> Filter) {
        entries += names to block()
    }

    /**
     * 添加正则条件
     *
     * @param pattern 正则表达式
     * @param block 根据匹配到的字符创建 Filter 的代码块
     */
    fun regex(pattern: String, block: (String) -> Filter) {
        regexes += pattern to block
    }

    /**
     * 动态查询条件
     *
     * @param block 条件的 DSL Builder
     */
    fun dynamic(block: ComboQueryBuilder.() -> Unit) {
        block()
    }

    /**
     * 添加一组别名与 Filter
     *
     * @param aliases 别名
     * @param filter Filter
     */
    fun add(aliases: List<String>, filter: Filter) {
        entries += aliases to filter
    }

    /**
     * 指定顺序添加一组别名与 Filter
     *
     * @param index 插入下标
     * @param aliases 别名
     * @param filter Filter
     */
    fun add(index: Int, aliases: List<String>, filter: Filter) {
        entries.add(index, aliases to filter)
    }

    /**
     * Combo 状态相关条件
     *
     * @param name 条件名称
     * @param sortBy 排序方式
     * @param block 条件代码块
     */
    fun combo(
        name: String? = null,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        block: (Record) -> Boolean = { true }
    ) = Filter(FilterType.Combo, record = block, sortBy = sortBy, name = name)

    /**
     * Chain 状态相关条件
     *
     * @param name 条件名称
     * @param block 条件代码块
     */
    fun sync(
        name: String? = null,
        block: (Record) -> Boolean = { true }
    ) = Filter(FilterType.Sync, record = block, name = name)

    /**
     * 达成率相关条件
     *
     * @param name 条件名称
     * @param sortBy 排序方式
     * @param block 条件代码块
     */
    fun achievement(
        name: String? = null,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        block: (Record) -> Boolean = { true }
    ) = Filter(FilterType.Achievement, record = block, sortBy = sortBy, name = name)

    /**
     * 修改成绩类条件
     *
     * @param name 条件名称
     * @param sortBy 排序方式
     * @param modifier 修改成绩
     */
    fun modification(
        name: String? = null,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        modifier: (Record.() -> Unit)? = null
    ) = Filter(FilterType.Modification, sortBy = sortBy, name = name, modifier = modifier)

    /**
     * 是否强制禁用 New 20
     *
     * @param disableN20 是否强制禁用 New 20
     */
    fun limit(disableN20: Boolean = false) =
        Filter(FilterType.Limit, disableN20 = disableN20)

    /**
     * 指定难度的条件
     *
     * @param difficulty 难度
     * @param name 条件名称
     */
    fun difficulty(difficulty: MusicDifficulty, name: String? = null) =
        Filter(FilterType.Difficulty, chart = { it.difficulty == difficulty }, singleChart = true, name = name)

    /**
     * 指定歌曲分类的条件
     *
     * @param genre 歌曲分类
     */
    fun genre(genre: MusicGenre) =
        Filter(FilterType.Genre, chart = { it.music.genre == genre })

    /**
     * 指定等级名的条件
     *
     * @param level 等级名
     */
    fun level(level: String) =
        Filter(FilterType.Level, chart = { it.level == level }, name = "level", singleChart = true)

    /**
     * 指定等级定数的条件
     *
     * @param levelValue 等级定数
     */
    fun levelValue(levelValue: Double) =
        Filter(FilterType.Level, chart = { it.levelValue == levelValue }, name = "levelValue", singleChart = true)

    /**
     * 指定版本的条件
     *
     * @param versions 版本列表
     */
    fun version(versions: List<GameVersion>) =
        Filter(FilterType.Version, chart = { it.music.version in versions })

    /**
     * 指定特定达成等级成绩的条件
     *
     * 需要严格等于该达成等级
     * @param rate 达成等级
     */
    fun rate(rate: String) =
        achievement { it.rate == rate }

    /**
     * 达到特定达成等级成绩的条件
     *
     * 可以大于等于该达成等级
     * @param rate 达成等级
     */
    fun rateGE(rate: String) =
        achievement { Rate.greaterEqual(it.achievement, rate) }

    /**
     * 指定最新版本的条件
     *
     * @param version 最新版本
     */
    fun nowVersion(version: GameVersion) =
        Filter(FilterType.Modification, chart = { it.music.version.version <= version.version }, nowVersion = { version })

    /**
     * 称号条件
     *
     * @param songIds 达成称号所需的歌曲 ID 列表
     * @param difficulties 达成称号所需的难度列表
     * @param rank 称号要求的达成等级
     * @param fullCombo 称号要求的连击状态
     * @param fullChain 称号要求的 Chain 状态
     * @param name 称号名称
     */
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
    }, name = "plate_" + when {
        fullCombo != null -> "combo_"
        fullChain != null -> "chain_"
        else -> ""
    } + name)
}