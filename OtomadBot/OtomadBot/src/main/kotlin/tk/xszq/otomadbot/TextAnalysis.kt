@file:Suppress("UNUSED", "CanBeParameter")
package tk.xszq.otomadbot

import com.huaban.analysis.jieba.JiebaSegmenter
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalStructure

enum class TextResult {
    SHOWING_WEAK,
    SENTIMENT_POSITIVE,
    SENTIMENT_NEUTRAL,
    SENTIMENT_NEGATIVE,
    CONTAINS_BAD_WORDS
}

fun Tree.getFirstLeaf(): Tree {
    var result = this
    while (!result.isLeaf) result = result.firstChild()
    return result
}

object TextAnalyser {
    private const val model: String = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz"
    private val parser: LexicalizedParser = LexicalizedParser.loadModel(model)
    private fun extractSubject(tree: Tree): String {
        var result = ""
        tree.reversed().forEach {
            if (it.label().value() in listOf("NN", "NR", "NT", "PN", "WP", "WP$")) {
                result = it.getFirstLeaf().value()
                return@forEach
            } else if (it.label().value() in listOf("VP", "VV")) {
                return@forEach
            }
        }
        return result
    }
    fun analyse(str: String): List<TextResult> {
        val result = mutableListOf<TextResult>()
        val weakWords = configMain.text["weak"]!!.split("|")
        if ("https" in str) {
            return emptyList()
        }
        val tree = parser.parse(JiebaSegmenter().sentenceProcess(str.toSimple()).joinToString(" "))
        val gramStructure = ChineseGrammaticalStructure(tree)
        val dependency = gramStructure.typedDependenciesCollapsed()
        // tree.pennPrint() // debug
        // println(dependency.toString()) // debug
        val subject = extractSubject(tree)
        // bot?.logger?.debug("主语：$subject") // debug
        var weak = false
        var weakWord = ""
        dependency.forEach dep@{ relation ->
            // bot?.logger?.info("gov: ${relation.gov()}; dep: ${relation.dep()}; ${relation.reln()}") // debug
            when (relation.reln().toString()) {
                in listOf("nsubj", "nsubjpass", "attr") -> {
                    weakWords.forEach { now ->
                        if (now in relation.gov().value()) {
                            weak = true
                            weakWord = relation.gov().value()
                        } else if (now in relation.dep().value()) {
                            weak = true
                            weakWord = relation.dep().value()
                        }
                    }
                }
            }
        }
        if (subject in configMain.text["self"]!! && weak) {
            bot?.logger?.debug("检测到卖弱词：$weakWord") // debug
            result.add(TextResult.SHOWING_WEAK)
        }
        return result
    }
}