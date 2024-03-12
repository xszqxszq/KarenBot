package xyz.xszq.nereides

import com.github.promeg.pinyinhelper.Pinyin
import com.github.promeg.tinypinyin.lexicons.java.cncity.CnCityDict
import korlibs.image.color.RGBA
import korlibs.memory.extract8
import xyz.xszq.config

fun String.toArgsList(): List<String> = this.trim().split(" +".toRegex()).toMutableList().filter { isNotBlank() }
fun List<String>.subArgsList(): List<String> {
    if (size < 2)
        return listOf()
    return subList(1, size)
}
fun String.toArgsListByLn(): List<String> = this.trim().split("\r\n", "\r", "\n").toMutableList().filter {
    isNotBlank()
}
fun String.toArgsListByLnOrSpace(): List<String> = if ("\r" in this || "\n" in this)
    toArgsListByLn()
else
    toArgsList()

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

fun String.toPinyinList() = Pinyin.toPinyin(this, ",").trim().split(",")

fun String.toPinyinAbbr(): String = toPinyinList().filter { it.isNotBlank() }.map { it.first() }.joinToString(separator="")

val domains = Regex("((?:[a-zA-Z][a-zA-Z0-9]{0,62}\\.)?[a-zA-Z][a-zA-Z0-9]{0,62}\\.[a-zA-Z0-9]{2,62})")
fun String.filterURL(whitelist: List<String> = config.domainWhitelist): String {
    var text = this
    domains.findAll(text).forEach {
        val domain = it.groupValues[0]
        if (domain !in whitelist)
            text = text.replace(it.groupValues[0], it.groupValues[0].replace(".", "·"))
    }
    return text
}

fun initPinyin() {
    Pinyin.init(Pinyin.newConfig().with(CnCityDict.getInstance()))
}