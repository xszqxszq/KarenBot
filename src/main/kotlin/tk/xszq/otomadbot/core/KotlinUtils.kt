@file:Suppress("unused")

package tk.xszq.otomadbot.core

import com.github.houbb.opencc4j.util.ZhConverterUtil
import okhttp3.ResponseBody
import org.jetbrains.exposed.sql.*

const val availableUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/85.0.4183.83 Safari/537.36"
val pass = {}

fun String.escape(): String {
    return replace("\n", "\\n")
}
fun String.isEmptyChar(): Boolean {
    val text = trim()
    return text == "" || text == "\n" || text == "\t"
}
fun String.toSimple(): String = ZhConverterUtil.toSimple(this)
fun String.trimLiteralTrident() = this.replace("    ", "")

class RegexpOpCol<T : String?>(
    /** Returns the expression being checked. */
    private val expr1: Expression<T>,
    /** Returns the regular expression [expr1] is checked against. */
    private val expr2: String
) : Op<Boolean>(), ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append(expr1, " REGEXP `$expr2`")
        }
    }
}
class InStr(expr1: String, expr2: Expression<*>): ComparisonOp(
    CustomStringFunction("INSTR", stringParam(expr1), expr2), intParam(0), "<>")
fun ResponseBody.get(): String = string()
