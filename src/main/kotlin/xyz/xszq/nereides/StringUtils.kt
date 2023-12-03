package xyz.xszq.nereides

import com.soywiz.kmem.extract8
import com.soywiz.korim.color.RGBA

fun String.toArgsList(): List<String> = this.trim().split(" +".toRegex()).toMutableList().filter { isNotBlank() }
fun String.toArgsListByLn(): List<String> = this.trim().split("\n").toMutableList().filter {
    isNotBlank()
}

const val DBC_SPACE = 32
const val SBC_SPACE = 12288
const val DBC_CHAR_START = 33
const val DBC_CHAR_END = 126
const val SBC_CHAR_START = 65281
const val SBC_CHAR_END = 65374
const val CONVERT_STEP = 65248
fun String.toDBC(): String {
    val buf = StringBuilder(length)
    this.toCharArray().forEach {
        buf.append(
            when (it.code) {
                SBC_SPACE -> DBC_SPACE
                in SBC_CHAR_START..SBC_CHAR_END -> it - CONVERT_STEP
                else -> it
            }
        )
    }
    return buf.toString()
}
fun String.toSBC(): String {
    val buf = StringBuilder(length)
    this.toCharArray().forEach {
        buf.append(
            when (it.code) {
                DBC_SPACE -> SBC_SPACE
                in DBC_CHAR_START..DBC_CHAR_END -> it + CONVERT_STEP
                else -> it
            }
        )
    }
    return buf.toString()
}

fun Char.isDBC() = this.code in DBC_SPACE..DBC_CHAR_END

fun String.limitDecimal(limit: Int = 4): String {
    if (toDoubleOrNull() == null)
        throw IllegalArgumentException("Only decimal String is allowed")
    var result = substringBefore('.') + '.'
    val afterPart = substringAfter('.')
    result += if (afterPart.length <= limit)
        afterPart + "0".repeat(4 - afterPart.length)
    else
        afterPart.substring(0, limit)
    return result
}

fun String.hexToRGBA(): RGBA {
    val int = substring(1).toInt(16)
    return RGBA.Companion.invoke(int.extract8(16), int.extract8(8), int.extract8(0), 0xff)
}