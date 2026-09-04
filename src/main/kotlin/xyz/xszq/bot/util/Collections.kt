package xyz.xszq.bot.util

import korlibs.math.toIntCeil
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

fun <T> List<T>.pagination(page: Int, pageSize: Int): Triple<List<T>, Int, Int> {
    if (isEmpty())
        return Triple(this, 0, 0)
    val totalPages = (size.toDouble() / pageSize).toIntCeil()
    val actualPage = if (page > totalPages) totalPages else max(1, page)
    val beginIndex = (actualPage - 1) * pageSize
    val endIndex = min(actualPage * pageSize, size)
    return Triple(subList(beginIndex, endIndex), actualPage, totalPages)
}

fun <A, B> MutableList<Pair<A, B>>.add(a: A, b: B) = add(Pair(a, b))

/**
 * 异步并行版 forEach
 *
 * @param block 要执行的代码块
 */
suspend fun <T> Collection<T>.forEachParallel(
    block: suspend (T) -> Unit
) = coroutineScope {
    forEach { item ->
        launch {
            block(item)
        }
    }
}