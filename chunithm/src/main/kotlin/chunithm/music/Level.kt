package xyz.xszq.bot.chunithm.music

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 等级及定数
 */
@Suppress("unused")
object Level {
    var levelRange = 1..15
    var plusRange = 7..15

    /**
     * 根据等级名得到定数取值范围
     *
     * @param level 等级名
     * @return 定数取值范围
     */
    fun toRange(level: String): ClosedFloatingPointRange<Double> {
        val plus = level.endsWith("+")
        val num =
            if (plus) level.substring(0 until level.length - 1).toInt()
            else level.toInt()
        if (plus) {
            val begin = num.toDouble() + 0.5
            val end = num.toDouble() + 0.9
            return begin .. end
        } else {
            val begin = num.toDouble()
            val end = num.toDouble() + 0.4
            return begin .. end
        }
    }
    /**
     * 等级名中的数字部分
     *
     * @param level 等级名
     * @return 等级数字
     */
    fun numberPart(level: String) = level.filter { it.isDigit() }.toIntOrNull() ?: 0
    /**
     * 比较等级高低
     */
    val comparator: (String, String) -> Int = { level1, level2 ->
        val a = if (level1.endsWith("?")) level1.substringBefore("?") else level1
        val b = if (level2.endsWith("?")) level2.substringBefore("?") else level2
        val num1 =
            if (a.endsWith("+")) a.substring(0..a.length-2).toDouble() + 0.5
            else a.toDouble()
        val num2 =
            if (b.endsWith("+")) b.substring(0..b.length-2).toDouble() + 0.5
            else b.toDouble()
        when {
            num1 < num2 -> -1
            num1 == num2 -> 0
            else -> 1
        }
    }
    /**
     * 全部等级
     */
    val levels: List<String> get() = buildList {
        levelRange.forEach { level ->
            add(level.toString())
            if (level in plusRange)
                add("$level+")
        }
    }
    /**
     * 全部定数
     */
    val levelValues: List<Double> get() = buildList {
        levelRange.forEach { level ->
            add(level.toDouble())
            if (level in plusRange) {
                repeat(9) { index ->
                    val decimal = (index + 1) / 10.0
                    add(level + decimal)
                }
            }
        }
    }
    /**
     * 清理多余小数
     */
    fun Double.levelClean() = (max(0.0, this) * 10).roundToInt() / 10.0
}