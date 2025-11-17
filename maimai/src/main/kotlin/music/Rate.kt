package xyz.xszq.bot.music

@Suppress("unused")
object Rate {
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
    fun greater(a: String, b: String) = floor(a) > floor(b)
    fun greater(a: Int, rate: String) = a > floor(rate)
    fun greaterEqual(a: Int, rate: String) = a >= floor(rate)
    fun toString(acc: Int): String = buildString {
        append(acc / 10000)
        append('.')
        append((acc % 10000).toString().padStart(4, '0'))
        append('%')
    }
}