package xyz.xszq.bot.chunithm.music

import java.text.DecimalFormat

/**
 * 达成等级
 */
@Suppress("unused")
object Rate {
    /**
     * 所有达成等级
     */
    val rates = listOf(
        "sssp", "sss", "ssp", "ss", "sp", "s",
        "aaa", "aa", "a", "bbb", "bb", "b",
        "c", "d"
    )

    /**
     * 根据达成率计算达成等级
     *
     * @param achievement 达成率
     * @return 达成等级
     */
    operator fun get(achievement: Int) = when (achievement) {
        in 1009000 .. 1010000 -> "sssp"
        in 1007500 until 1009000 -> "sss"
        in 1005000 until 1007500 -> "ssp"
        in 1000000 until 1005000 -> "ss"
        in 990000 until 1000000 -> "sp"
        in 975000 until 990000 -> "s"
        in 950000 until 975000 -> "aaa"
        in 925000 until 950000 -> "aa"
        in 900000 until 925000 -> "a"
        in 800000 until 900000 -> "bbb"
        in 700000 until 800000 -> "bb"
        in 600000 until 700000 -> "b"
        in 500000 until 600000 -> "c"
        else -> "d"
    }
    /**
     * 根据达成等级计算最低达成率
     *
     * @param rate 达成等级
     * @return 最低达成率
     */
    fun floor(rate: String) = when (rate) {
        "sssp" -> 1009000
        "sss" -> 1007500
        "ssp" -> 1005000
        "ss" -> 1000000
        "sp" -> 990000
        "s" -> 975000
        "aaa" -> 950000
        "aa" -> 925000
        "a" -> 900000
        "bbb" -> 800000
        "bb" -> 700000
        "b" -> 600000
        "c" -> 500000
        else -> 0
    }
    /**
     * 比较达成等级的高低
     *
     * @param a 达成等级
     * @param b 达成等级
     * @return a 是否高于 b
     */
    fun greater(a: String, b: String) = floor(a) > floor(b)

    /**
     * 判断达成率是否达成该达成等级
     *
     * @param a 达成率
     * @param rate 达成等级
     * @return 是否达成
     */
    fun greater(a: Int, rate: String) = a > floor(rate)

    /**
     * 判断达成率是否不低于该达成等级
     *
     * @param a 达成率
     * @param rate 达成等级
     * @return 是否不低于
     */
    fun greaterEqual(a: Int, rate: String) = a >= floor(rate)

    /**
     * 把整数拆成两段带千分位的展示文本
     *
     * @param value 待拆分的整数
     * @return 拆分后的两段文本
     */
    fun formatted(value: Int): Pair<String, String> {
        val raw = DecimalFormat("#,##0").format(value)
        return when (val index = raw.lastIndexOf(',')) {
            -1 -> Pair(raw, "")
            else -> {
                Pair(raw.substring(0, index + 1), raw.substring(index + 1))
            }
        }
    }
    /**
     * 返回下一个达成等级
     *
     * @param rate 达成等级
     * @return 下一个达成等级
     */
    fun next(rate: String) = when (rate) {
        "sssp" -> "sssp"
        else -> rates[rates.indexOf(rate) - 1]
    }
}