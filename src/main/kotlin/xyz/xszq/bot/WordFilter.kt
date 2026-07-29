package xyz.xszq.bot

class WordFilter(
    val words: List<String>
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
    fun filter(text: String): String {
        if (regex == null)
            return text
        return regex.replace(text) { matchResult ->
            "*".repeat(matchResult.value.length)
        }
    }
}