import org.xm.Similarity
import xyz.xszq.bot.touhou.Touhou.Companion.SIMILAR_THRESHOLD

fun isSimilar(
    a: String,
    b: String
): Boolean {
    println(a)
    println(b)
    println(Similarity.cilinSimilarity(a, b))
    println(Similarity.pinyinSimilarity(a, b))
    println(Similarity.charBasedSimilarity(a, b))
    return Similarity.cilinSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.pinyinSimilarity(a, b) > SIMILAR_THRESHOLD ||
            Similarity.charBasedSimilarity(a, b) > SIMILAR_THRESHOLD
}

fun test() {
    var answers = listOf("梦消失　～lost dream", "梦消失 ～ lost dream", "vanishing dream ~ lost dream", "卡娜·安娜贝拉尔",
        "东方梦时空梦消失　～lost dream", "梦时空梦消失　～lost dream", "msk梦消失　～lost dream", "东方梦时空梦消失 ～ lost dream",
        "梦时空梦消失 ～ lost dream", "msk梦消失 ～ lost dream", "东方梦时空vanishing dream ~ lost dream",
        "梦时空vanishing dream ~ lost dream", "mskvanishing dream ~ lost dream", "东方梦时空卡娜·安娜贝拉尔",
        "梦时空卡娜·安娜贝拉尔", "msk卡娜·安娜贝拉尔").toMutableList()
    answers.filter { "～" in it }.forEach { before ->
        before.split("～").map { it.trim() }.forEach {
            answers.add(it)
        }
    }
    println(answers)
}

fun main() {
    test()
}