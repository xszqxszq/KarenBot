package xyz.xszq.bot.chunithm.music

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

object Rating {
    fun color(rating: Double) = when (rating) {
        in 0.00 .. 11.99 -> 1
        in 12.00 .. 13.24 -> 2
        in 13.25 .. 14.49 -> 3
        in 14.50 .. 15.24 -> 4
        in 15.25 .. 15.99 -> 5
        in 16.00 .. 16.99 -> 6
        in 17.00 .. 20.00 -> 7
        else -> 1
    }
    fun stringWithoutDot(value: Double): String = "%04d".format((value * 100).roundToInt())
    fun calc(chart: ChartInfo, achievement: Int) = calc(chart.levelValue, achievement)
    fun calc(levelValue: Double, achievement: Int) = when (val rate = Rate[achievement]) {
        "sssp" -> levelValue + 2.15
        "sss" -> levelValue + 2.0 + (achievement - Rate.floor(rate)) / 100 * 0.01
        "ssp" -> levelValue + 1.5 + (achievement - Rate.floor(rate)) / 50 * 0.01
        "ss" -> levelValue + 1.0 + (achievement - Rate.floor(rate)) / 100 * 0.01
        "sp" -> levelValue + 0.6 + (achievement - Rate.floor(rate)) / 250 * 0.01
        "s" -> levelValue + (achievement - Rate.floor(rate)) / 250 * 0.01
        "aaa" -> levelValue - 1.67 + (achievement - Rate.floor(rate)) / 150 * 0.01
        "aa" -> levelValue - 3.34 + (achievement - Rate.floor(rate)) / 150 * 0.01
        "a" -> levelValue - 5.0 + (achievement - Rate.floor(rate)) / 150 * 0.01
        "bbb" -> (levelValue - 5.0) / 2 + ((achievement - Rate.floor(rate)) * (levelValue - 5.0) / 2000).toInt() * 0.01
        "bb", "b", "c" -> ((achievement - Rate.floor("c")) * (levelValue - 5.0) / 6000).toInt() * 0.01
        else -> 0.0
    }.ratingClean()
    fun Double.ratingClean() = (max(0.0, this) * 100).roundToInt() / 100.0
    fun Double.ratingFloor() = floor(this * 100) / 100.0
}