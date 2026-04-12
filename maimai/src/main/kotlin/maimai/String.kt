package xyz.xszq.bot.maimai

import com.github.houbb.opencc4j.util.ZhConverterUtil

fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.endsWith(target: List<String>) = target.any { endsWith(it) }
fun String.substringBefore(target: List<String>) =
    target.firstOrNull { endsWith(it) }
        ?.let { substringBefore(it) }
        ?: this