package xyz.xszq.bot.chunithm.music

import java.text.DecimalFormat

@Suppress("unused")
object Rate {
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
    fun greater(a: String, b: String) = floor(a) > floor(b)
    fun greater(a: Int, rate: String) = a > floor(rate)
    fun greaterEqual(a: Int, rate: String) = a >= floor(rate)
    fun formatted(value: Int): Pair<String, String> {
        val raw = DecimalFormat("#,##0").format(value)
        return when (val index = raw.lastIndexOf(',')) {
            -1 -> Pair(raw, "")
            else -> {
                Pair(raw.substring(0, index + 1), raw.substring(index + 1))
            }
        }
    }
}