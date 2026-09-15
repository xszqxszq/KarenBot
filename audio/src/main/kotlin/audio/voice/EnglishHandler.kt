package xyz.xszq.bot.audio.voice

import xyz.xszq.g2p.EnglishPhonemizer

/**
 * 英文单词转换
 */
class EnglishHandler(
    val phonemizer: EnglishPhonemizer
) {

    /**
     * 汉语元音表
     */
    private val vowels = arrayOf('a', 'e', 'i', 'o', 'u')

    /**
     * 汉语辅音表
     */
    private val consonants = arrayOf(
        'b', 'p', 'm', 'f',
        'd', 't', 'n', 'l',
        'g', 'k', 'h', 'w',
        'j', 'q', 'x',
        'z', 's', 'r', 'y',
    )

    /**
     * 单个辅音到汉语拼音对照表
     */
    private val consonantTable = mapOf(
        "b" to "bu", "p" to "pu", "m" to "mu", "f" to "fu",
        "d" to "de", "t" to "te", "n" to "en", "l" to "er",
        "g" to "ge", "k" to "ke", "h" to "he", "w" to "wu",
        "j" to "ji", "q" to "qi", "x" to "xi", "z" to "zi",
        "s" to "si", "r" to "er", "y" to "yi"
    )
    /**
     * 汉语无效拼音到相近音位对照表
     */
    private val invalidTable = mapOf(
        "be" to "bei", "pe" to "pei","me" to "mei","fe" to "fei",
        "do" to "duo", "to" to "tuo","no" to "nuo","lo" to "luo",
        "go" to "gou", "ko" to "kou", "ho" to "hou",
        "ja" to "jia", "je" to "jie", "jo" to "jiu",
        "qa" to "qia", "qe" to "qie", "qo" to "qiu",
        "xa" to "xia", "xe" to "xie", "xo" to "xiu",
        "i" to "yi", "o" to "ou", "u" to "wu",
        "zo" to "zou", "so" to "sou",
        "ra" to "la", "ro" to "rou",
        "we" to "wei",
        "ten" to "teng",
        "yo" to "you",
    )
    /**
     * 单个英文字母到汉语拼音对照表
     */
    private val letterNamePinyin = mapOf(
        "a" to arrayOf("ei"), "b" to arrayOf("bi"), "c" to arrayOf("xi"),
        "d" to arrayOf("di"), "e" to arrayOf("yi"), "f" to arrayOf("ai", "fu"),
        "g" to arrayOf("ji"), "h" to arrayOf("ai", "chi"), "i" to arrayOf("ai"),
        "j" to arrayOf("jie"), "k" to arrayOf("ke", "ei"), "l" to arrayOf("ai", "lu"),
        "m" to arrayOf("ai", "mu"), "n" to arrayOf("en"), "o" to arrayOf("ou"),
        "p" to arrayOf("pi"), "q" to arrayOf("ke","you"), "r" to arrayOf("a"),
        "s" to arrayOf("ai", "si"), "t" to arrayOf("ti"), "u" to arrayOf("you"),
        "v" to arrayOf("wei"), "w" to arrayOf("da", "bu", "liu"), "x" to arrayOf("ai", "ke", "si"),
        "y" to arrayOf("wai"), "z" to arrayOf("ze", "ei")
    )

    /**
     * 英文单词转为汉语拼音
     *
     * @param wordRaw 原始英文单词
     * @return 汉语拼音
     */
    fun convertWord(wordRaw: String): Array<String> {
        val word = wordRaw.trim()
        if (word.length == 1 && word[0].isLetter()) {
            return letterNamePinyin[word.lowercase()] ?: arrayOf(word.lowercase())
        }

        val phones = phonesForWord(word)
        val pre = preprocessPhones(phones)
        val norm = mapToLatinPieces(pre)
        val syll = assembleWithTables(norm)
        val fixed = syll.map { invalidTable[it] ?: it }.toTypedArray()
        return mergeStandaloneNg(fixed).map { it.lowercase() }.toTypedArray()
    }

    private fun phonesForWord(text: String): Array<String> =
        phonemizer.phonemize(text).toTypedArray()

    private fun preprocessPhones(raw: Array<String>): Array<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < raw.size) {
            val cur = raw[i]
            val nxt = raw.getOrNull(i + 1)
            if (cur == "j" && nxt in listOf("O", "oU", "U", "@", "3`")) {
                out += "YOU"
                i += 2
                continue
            }
            if (cur.endsWith("=")) {
                out += "SYLL_${cur.removeSuffix("=")}"
                i += 1
                continue
            }
            out += cur
            i += 1
        }
        return out.toTypedArray()
    }

    /**
     * 英文音素到汉语拼音对照表
     */
    private val phoneMap = mapOf(
        "aI" to "ai", "aU" to "ao", "OI" to "ui",
        "O" to "o", "oU" to "ou",
        "i" to "i", "I" to "i",
        "e" to "e", "eI" to "ei", "E" to "e",
        "A" to "a", "a" to "a", "{" to "a",
        "V" to "a", "@" to "e", "3`" to "er",
        "p" to "p", "b" to "b", "t" to "t",
        "d" to "d", "k" to "k", "g" to "g",
        "f" to "f", "v" to "w",
        "s" to "s", "z" to "z",
        "S" to "x", "Z" to "j",
        "tS" to "q", "dZ" to "j",
        "T" to "s", "D" to "z",
        "h" to "h", "w" to "w", "j" to "y",
        "l" to "l", "m" to "m", "n" to "n", "N" to "ng",
        "r" to "r"
    )
    private fun mapToLatinPieces(pre: Array<String>): Array<String> =
        pre.flatMap { p ->
            when (p) {
                "YOU" -> listOf("you")
                "SYLL_r" -> listOf("er")
                else -> listOf(phoneMap[p] ?: p)
            }
        }.toTypedArray()

    private fun isVowel(tok: String): Boolean =
        (tok.length == 1 && tok[0] in vowels) ||
            tok in setOf("ai", "ei", "ao", "ou", "ui", "er", "you", "ng")

    private fun isConsonant(tok: String): Boolean =
        (tok.length == 1 && tok[0] in consonants)

    private fun assembleWithTables(pieces: Array<String>): Array<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < pieces.size) {
            val cur = pieces[i]
            val nxt = pieces.getOrNull(i + 1)

            when {
                isVowel(cur) && !isConsonant(cur) -> { out += cur; i++ }
                isConsonant(cur) && nxt == "er" && cur != "r" -> {
                    out += (invalidTable[cur + "e"] ?: (cur + "e"))
                    i += 1
                }
                isConsonant(cur) && nxt != null && isVowel(nxt) -> {
                    val syll = cur + nxt
                    out += (invalidTable[syll] ?: syll)
                    i += 2
                }
                isConsonant(cur) -> {
                    val backoff = consonantTable[cur] ?: (cur + "e")
                    out += (invalidTable[backoff] ?: backoff)
                    i += 1
                }
                else -> { out += (invalidTable[cur] ?: cur); i += 1 }
            }
        }
        return out.toTypedArray()
    }

    private fun mergeStandaloneNg(tokens: Array<String>): Array<String> {
        if (tokens.isEmpty()) return tokens
        val out = mutableListOf<String>()
        for (t in tokens) {
            if (t == "ng" && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + "ng"
            } else out += t
        }
        return out.toTypedArray()
    }
}