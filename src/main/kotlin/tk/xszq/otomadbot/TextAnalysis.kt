@file:Suppress("UNUSED", "CanBeParameter")
package tk.xszq.otomadbot

import com.huaban.analysis.jieba.JiebaSegmenter

enum class TextResult {
    SHOWING_WEAK,
    SENTIMENT_POSITIVE,
    SENTIMENT_NEUTRAL,
    SENTIMENT_NEGATIVE,
    CONTAINS_BAD_WORDS
}

object TextAnalyser {
    private val personalPronuns = listOf("我", "俺", "老子", "劳资", "本人", "自己")
    fun analyse(str: String): List<TextResult> {
        if ("://" in str) { // Do not handle links
            return emptyList()
        }
        val result = mutableSetOf<TextResult>()
        val words = mutableListOf<String>()
        JiebaSegmenter().sentenceProcess(str.toSimple()).forEach { raw ->
            personalPronuns.matchString(raw) ?.let { now ->
                words.addAll(listOf(raw.substringBefore(now), now, raw.substringAfter(now)).filter{ it.isNotBlank() })
            } ?: run {
                words.add(raw)
            }
        }
        val weakWords = configMain.text["weak"]!!.split("|")
        var ppDetected = false
        words.forEach { word ->
            if (word in personalPronuns) {
                ppDetected = true
            } else {
                weakWords.matchString(word) ?.let {
                    if (ppDetected) {
                        result.add(TextResult.SHOWING_WEAK)
                        bot?.logger?.debug("检测到卖弱词：$it") // debug
                    }
                }
            }
        }
        return result.toList()
    }
}