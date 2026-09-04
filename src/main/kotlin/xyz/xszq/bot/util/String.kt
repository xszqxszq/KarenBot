@file:Suppress("unused")

package xyz.xszq.bot.util

import kotlinx.serialization.json.Json
import xyz.xszq.bot.event.Event
import xyz.xszq.bot.event.MessageEvent

/**
 * 全局 JSON 对象
 */
val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

const val DBC_SPACE = 32
const val SBC_SPACE = 12288
const val DBC_CHAR_START = 33
const val DBC_CHAR_END = 126
const val SBC_CHAR_START = 65281
const val SBC_CHAR_END = 65374
const val CONVERT_STEP = 65248

/**
 * 将字符串中的全角字符转为半角
 */
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

/**
 * 将字符串中的半角字符转为全角
 */
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

/**
 * 判断字符串是否包含英文字母
 */
fun String.hasAlpha() = any { it.category in listOf(CharCategory.LOWERCASE_LETTER, CharCategory.UPPERCASE_LETTER) }

/**
 * 清理消息文本
 *
 * 去除首尾空白、父前缀及开头的“/”
 *
 * @param event 待处理事件
 * @param parent 消息前缀
 * @param forceParent 消息是否需以 parent 开头
 * @return 结果文本
 */
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