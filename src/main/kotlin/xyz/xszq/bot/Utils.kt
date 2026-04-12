package xyz.xszq.bot

import korlibs.math.toIntCeil
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent
import kotlin.math.max
import kotlin.math.min

typealias ErrorHandler = suspend MessageEvent.(Throwable) -> Unit


fun <T> List<T>.pagination(page: Int, pageSize: Int): Triple<List<T>, Int, Int> {
    if (isEmpty())
        return Triple(this, 0, 0)
    val totalPages = (size.toDouble() / pageSize).toIntCeil()
    val actualPage = if (page > totalPages) totalPages else max(1, page)
    val beginIndex = (actualPage - 1) * pageSize
    val endIndex = min(actualPage * pageSize, size)
    return Triple(subList(beginIndex, endIndex), actualPage, totalPages)
}

fun <A, B> MutableList<Pair<A, B>>.add(a: A, b: B) = add(Pair(a, b))

fun String.hasAlpha() = any { it.category in listOf(CharCategory.LOWERCASE_LETTER, CharCategory.UPPERCASE_LETTER) }

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

operator fun String.times(times: Int): String = buildString {
    repeat(times) {
        append(this@times)
    }
}
operator fun Char.times(times: Int): String = buildString {
    repeat(times) {
        append(this@times)
    }
}

fun normalizeMessage(
    event: Event,
    parent: String? = null,
    forceParent: Boolean = false
): Pair<MessageEvent, String>? {
    if (event !is MessageEvent)
        return null

    var message = event.text.trim()
    parent?.let {
        if (message.startsWith(it))
            message = message.substringAfter(it).trim()
        else if (forceParent)
            return null
    }
    message = message.removePrefix("/").trim()
    return event to message
}