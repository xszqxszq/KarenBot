package xyz.xszq.bot.maimai.music

import korlibs.math.toIntFloor
import kotlin.math.min

object Rating {
    fun color(rating: Int) = when (rating) {
        in 0 until 1000 -> 1
        in 1000 until 2000 -> 2
        in 2000 until 4000 -> 3
        in 4000 until 7000 -> 4
        in 7000 until 10000 -> 5
        in 10000 until 12000 -> 6
        in 12000 until 13000 -> 7
        in 13000 until 14000 -> 8
        in 14000 until 14500 -> 9
        in 14500 until 15000 -> 10
        in 15000 until 20000 -> 11
        else -> 1
    }

    fun calc(chart: ChartInfo, achievement: Int): Int = calc(chart.levelValue, achievement)
    fun calc(levelValue: Double, achievement: Int): Int {
        val baseRa = when (Rate[achievement]) {
            "d" -> 7.0
            "c" -> 8.0
            "b" -> 9.6
            "bb" -> 11.2
            "bbb" -> if (achievement == 799999) 12.8 else 12.0
            "a" -> 13.6
            "aa" -> 15.2
            "aaa" -> if (achievement == 969999) 17.6 else 16.8
            "s" -> 20.0
            "sp" -> if (achievement == 989999) 20.6 else 20.3
            "ss" -> 20.8
            "ssp" -> if (achievement == 999999) 21.4 else 21.1
            "sss" -> if (achievement == 1004999) 22.2 else 21.6
            "sssp" -> 22.4
            else -> 0.0
        }
        return (levelValue * baseRa * min(1005000, achievement) / 1000000.0).toIntFloor()
    }

    fun colorOld(rating: Int) = when (rating) {
        in 0 until 1000 -> 1
        in 1000 until 2000 -> 2
        in 2000 until 3000 -> 3
        in 3000 until 4000 -> 4
        in 4000 until 5000 -> 5
        in 5000 until 6000 -> 6
        in 6000 until 7000 -> 7
        in 7000 until 8000 -> 8
        in 8000 until 8500 -> 9
        in 8500 until 20000 -> 11
        else -> 1
    }

    fun courseOld(course: Int) = when (course) {
        0 -> 0
        1 -> 1000
        2 -> 1200
        3 -> 1400
        4 -> 1500
        5 -> 1600
        6 -> 1700
        7 -> 1800
        8 -> 1850
        9 -> 1900
        10 -> 1950
        11 -> 2000
        12 -> 2010
        13 -> 2020
        14 -> 2030
        15 -> 2040
        16 -> 2050
        17 -> 2060
        18 -> 2070
        19 -> 2080
        20 -> 2090
        21 -> 2100
        22 -> 2100
        23 -> 2100
        else -> 0
    }

    fun calcOld(chart: ChartInfo, achievement: Int): Int = calcOld(chart.levelValue, achievement)
    fun calcOld(levelValue: Double, achievement: Int): Int {
        val baseRa = when (Rate[achievement]) {
            "d" -> 0.0
            "c" -> 5.0
            "b" -> 6.0
            "bb" -> 7.0
            "bbb" -> 7.5
            "a" -> 8.5
            "aa" -> 9.5
            "aaa" -> 10.5
            "s" -> 12.5
            "sp" -> 12.7
            "ss" -> 13.0
            "ssp" -> 13.2
            "sss" -> 13.5
            "sssp" -> 14.0
            else -> 0.0
        }
        return (levelValue * baseRa * min(1005000, achievement) / 1000000.0).toIntFloor()
    }
}