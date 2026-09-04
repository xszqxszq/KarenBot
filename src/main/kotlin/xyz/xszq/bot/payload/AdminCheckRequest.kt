package xyz.xszq.bot.payload

import kotlinx.coroutines.CompletableDeferred

/**
 * 管理员判断请求
 *
 * @property deferred 判断结果
 */
data class AdminCheckRequest(
    val userId: String,
    val deferred: CompletableDeferred<Boolean>
)