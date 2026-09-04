package xyz.xszq.bot.service

/**
 * 敏感词过滤
 *
 * @param words 敏感词列表
 */
class WordFilter(
    words: List<String>
) {
    private val regex = if (words.isNotEmpty()) {
        val pattern = words.filter {
            it.isNotEmpty()
        }.sortedByDescending {
            it.length
        }.joinToString("|") {
            Regex.escape(it)
        }
        Regex(pattern, RegexOption.IGNORE_CASE)
    } else null

    /**
     * 过滤文本中的敏感词
     *
     * @param text 原始文本
     * @return 过滤后的文本
     */
    fun filter(text: String): String {
        if (regex == null)
            return text
        return regex.replace(text) { matchResult ->
            "*".repeat(matchResult.value.length)
        }
    }
}