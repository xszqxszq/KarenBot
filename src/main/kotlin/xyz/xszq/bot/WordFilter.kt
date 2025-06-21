package xyz.xszq.bot

class WordFilter(
    val words: List<String>
) {
    fun filter(text: String): String {
        var result = text
        words.forEach { word ->
            result = result.replace(word, '*' * word.length)
        }
        return result
    }
}