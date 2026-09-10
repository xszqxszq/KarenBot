package xyz.xszq.bot.util

import kotlinx.coroutines.delay
import xyz.xszq.bot.exception.RetryException

/**
 * 重试指定次数直至成功
 *
 * @param times 最大重试次数
 * @param block 代码块
 * @return 首次非空结果
 */
inline fun <T> retry(times: Int, block: () -> T): T? {
    (1..times).forEach { attempt ->
        block() ?.let {
            return it
        }
    }
    return null
}

/**
 * 异步重试直至成功
 *
 * 若仍未成功则抛出最后一次的异常
 *
 * @param times 最大尝试次数
 * @param block 代码块
 * @return 执行结果
 */
suspend fun <T> retryAsync(
    times: Int,
    block: suspend (attempt: Int) -> T
): T {
    var attempt = 0
    while (true) {
        attempt++
        val result = runCatching { block(attempt) }
        val e = result.exceptionOrNull() ?: return result.getOrThrow()
        if (e !is RetryException || attempt >= times)
            throw e
        delay(attempt * 2000L)
    }
}