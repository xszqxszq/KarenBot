package xyz.xszq.bot.maimai.query

import xyz.xszq.bot.maimai.music.*
import kotlin.random.Random

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
     * Sync 状态相关条件
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
     * @param fitLevelValue 是否用拟合定数
     * @param sortBy 排序方式
     * @param modifier 修改成绩
     */
    fun modification(
        name: String? = null,
        fitLevelValue: Boolean = false,
        sortBy: (Record) -> Comparable<*> = Filter.defaultSort,
        modifier: (Record.() -> Unit)? = null
    ) = Filter(FilterType.Modification, sortBy = sortBy, name = name,
        fitLevelValue = fitLevelValue, modifier = modifier)

    /**
     * 是否强制禁用 New 15
     *
     * @param disable15 是否强制禁用 New 15
     */
    fun limit(disable15: Boolean = false) =
        Filter(FilterType.Limit, disable15 = disable15)

    /**
     * 指定难度的条件
     *
     * @param difficulty 难度
     * @param name 条件名称
     */
    fun difficulty(
        difficulty: MusicDifficulty,
        name: String? = null
    ) = Filter(FilterType.Difficulty, chart = { it.difficulty == difficulty },
        singleChart = true, name = name)

    /**
     * 指定歌曲分类的条件
     *
     * @param genre 曲目类型
     */
    fun genre(genre: MusicGenre) =
        Filter(FilterType.Genre, chart = { it.music.genre == genre })

    /**
     * 指定等级名的条件
     *
     * @param level 等级名
     */
    fun level(level: String) =
        Filter(FilterType.Level, chart = { it.level == level },
            name = "level", singleChart = true)

    /**
     * 指定等级定数的条件
     *
     * @param levelValue 等级定数
     */
    fun levelValue(levelValue: Double) =
        Filter(FilterType.Level, chart = { it.levelValue == levelValue },
            name = "levelValue", singleChart = true)

    /**
     * 指定 DX 星数的条件
     *
     * @param stars DX 星数
     */
    fun stars(stars: Int) = Filter(FilterType.Star, record = {
        DeluxeScore.stars(it.deluxeScore, it.chart.maxDeluxeScore) == stars
    })

    /**
     * 指定谱面类型的条件
     *
     * @param type 谱面类型
     */
    fun type(type: MusicType) =
        Filter(FilterType.Type, chart = { it.music.type == type })

    /**
     * 指定版本的条件
     *
     * @param versions 版本列表
     */
    fun version(versions: List<GameVersion>) =
        Filter(FilterType.Version, chart = { it.music.version in versions })

    /**
     * 指定标签的条件
     *
     * @param musics 包含的歌曲 ID
     * @param name 条件名称
     */
    fun tag(musics: List<Int>, name: String? = null) =
        Filter(FilterType.Tag, chart = { it.music.id in musics }, name = name)

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
        Filter(FilterType.Version, chart = { it.music.version.version <= version.version },
            nowVersion = { version })

    /**
     * 随机排序条件
     *
     * @param random Random 对象
     */
    fun random(random: Random): Filter {
        val orders = mutableMapOf<Record, Int>()
        return Filter(FilterType.Sort, sortBy = { orders.getOrPut(it) { random.nextInt() } })
    }

    /**
     * 牌子条件
     *
     * @param musics 达成牌子所需的歌曲 ID 列表
     * @param reMasters 达成牌子所需的白谱的歌曲 ID 列表
     * @param name 牌子名称
     */
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