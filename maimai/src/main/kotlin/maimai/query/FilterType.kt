package xyz.xszq.bot.maimai.query

enum class FilterType {
    // 成绩类
    Achievement, Combo, Sync, Star,
    // 谱面信息类
    Difficulty, Level, Designer, Genre,
    Version, Type, Plate, Tag,
    // 排序类
    Sort,
    // 修改数据类,
    Modification,
    // 条件类
    Limit,
    // 默认过滤类
    Default;

    val matchesChart: Boolean get() = this in listOf(Difficulty, Level, Designer)
}