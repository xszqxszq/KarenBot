package xyz.xszq.bot.maimai.music

import korlibs.math.toIntFloor

object DeluxeScore {
    fun stars(deluxeScore: Int, maxScore: Int): Int {
        if (maxScore <= 0 || deluxeScore > maxScore)
            return 0
        val percent = (deluxeScore * 100.0 / maxScore).toIntFloor()
        return when (percent) {
            in 0 until 85 -> 0
            in 85 until 90 -> 1
            in 90 until 93 -> 2
            in 93 until 95 -> 3
            in 95 until 97 -> 4
            in 97 until 100 -> 5
            else -> 0
        }
    }
}