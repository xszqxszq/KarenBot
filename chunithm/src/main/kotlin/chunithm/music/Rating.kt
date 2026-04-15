package xyz.xszq.bot.chunithm.music

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
}