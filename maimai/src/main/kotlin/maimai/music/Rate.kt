package xyz.xszq.bot.maimai.music

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
        in 1005000 .. 1010000 -> "sssp"
        in 1000000 until 1005000 -> "sss"
        in 995000 until 1000000 -> "ssp"
        in 990000 until 995000 -> "ss"
        in 980000 until 990000 -> "sp"
        in 970000 until 980000 -> "s"
        in 940000 until 970000 -> "aaa"
        in 900000 until 940000 -> "aa"
        in 800000 until 900000 -> "a"
        in 750000 until 800000 -> "bbb"
        in 700000 until 750000 -> "bb"
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
        "sssp" -> 1005000
        "sss" -> 1000000
        "ssp" -> 995000
        "ss" -> 990000
        "sp" -> 980000
        "s" -> 970000
        "aaa" -> 940000
        "aa" -> 900000
        "a" -> 800000
        "bbb" -> 750000
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
     * 格式化达成率字符串
     *
     * @param acc 达成率
     * @return 格式化字符串
     */
    fun toString(acc: Int): String = buildString {
        append(acc / 10000)
        append('.')
        append((acc % 10000).toString().padStart(4, '0'))
        append('%')
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