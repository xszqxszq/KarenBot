package xyz.xszq.bot.audio.voice

import marytts.MaryInterface
import org.w3c.dom.Element

class EnglishHandler(
    val mary: MaryInterface
) {

    private val vowels = listOf('a','e','i','o','u')

    private val consonants = listOf(
        'b','p','m','f',
        'd','t','n','l',
        'g','k','h','w',
        'j','q','x',
        'z','s','r','y',
    )

    private val consonantTable = mapOf(
        "b" to "bu","p" to "pu","m" to "mu","f" to "fu",
        "d" to "de","t" to "te","n" to "en","l" to "er",
        "g" to "ge","k" to "ke","h" to "he","w" to "wu",
        "j" to "ji","q" to "qi","x" to "xi","z" to "zi",
        "s" to "si","r" to "er","y" to "yi"
    )
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
    private val letterNamePinyin = mapOf(
        "a" to listOf("ei"), "b" to listOf("bi"), "c" to listOf("xi"),
        "d" to listOf("di"), "e" to listOf("yi"), "f" to listOf("ai", "fu"),
        "g" to listOf("ji"), "h" to listOf("ai", "chi"), "i" to listOf("ai"),
        "j" to listOf("jie"), "k" to listOf("ke", "ei"), "l" to listOf("ai", "lu"),
        "m" to listOf("ai", "mu"), "n" to listOf("en"), "o" to listOf("ou"),
        "p" to listOf("pi"), "q" to listOf("ke","you"), "r" to listOf("a"),
        "s" to listOf("ai", "si"), "t" to listOf("ti"), "u" to listOf("you"),
        "v" to listOf("wei"), "w" to listOf("da", "bu", "liu"), "x" to listOf("ai", "ke", "si"),
        "y" to listOf("wai"), "z" to listOf("ze", "ei")
    )

    fun convertWord(wordRaw: String): List<String> {
        val word = wordRaw.trim()
        if (word.length == 1 && word[0].isLetter()) {
            return letterNamePinyin[word.lowercase()] ?: listOf(word.lowercase())
        }

        val maryPhones = phonesForWord(word)
        val pre = preprocessPhones(maryPhones)
        val norm = mapToLatinPieces(pre)
        val syll = assembleWithTables(norm)
        val fixed = syll.map { invalidTable[it] ?: it }
        return mergeStandaloneNg(fixed).map { it.lowercase() }
    }

    private fun phonesForWord(text: String): List<String> {
        val document = mary.generateXML(text)
        val phones = mutableListOf<String>()

        val nodes = document.getElementsByTagNameNS("*", "ph")
        (0 until nodes.length).forEach { i ->
            val element = nodes.item(i) as? Element ?: return@forEach
            val p = element.getAttribute("p").ifBlank { element.textContent ?: "" }.trim()
            if (p.isNotBlank()) {
                p.trim().split(Regex("\\s+")).forEach {
                    if (it.isNotBlank())
                        phones.add(it)
                }
            }
        }
        if (phones.isEmpty()) {
            val ts = document.getElementsByTagNameNS("*", "t")
            (0 until ts.length).forEach { i ->
                val element = ts.item(i) as? Element ?: return@forEach
                val p = element.getAttribute("ph").trim()
                if (p.isNotBlank()) {
                    p.trim().split(Regex("\\s+")).forEach {
                        if (it.isNotBlank())
                            phones.add(it)
                    }
                }
            }
        }
        return phones
    }

    private fun preprocessPhones(raw: List<String>): List<String> {
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
        return out
    }

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
    private fun mapToLatinPieces(pre: List<String>): List<String> =
        pre.flatMap { p ->
            when (p) {
                "YOU" -> listOf("you")
                "SYLL_r" -> listOf("er")
                else -> listOf(phoneMap[p] ?: p)
            }
        }

    private fun isVowel(tok: String): Boolean =
        (tok.length == 1 && tok[0] in vowels) ||
            tok in setOf("ai", "ei", "ao", "ou", "ui", "er", "you", "ng")

    private fun isConsonant(tok: String): Boolean =
        (tok.length == 1 && tok[0] in consonants)

    private fun assembleWithTables(pieces: List<String>): List<String> {
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
        return out
    }

    private fun mergeStandaloneNg(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return tokens
        val out = mutableListOf<String>()
        for (t in tokens) {
            if (t == "ng" && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + "ng"
            } else out += t
        }
        return out
    }
}