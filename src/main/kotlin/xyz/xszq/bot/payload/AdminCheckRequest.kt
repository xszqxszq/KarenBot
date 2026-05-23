package xyz.xszq.bot.payload

import kotlinx.coroutines.CompletableDeferred

data class AdminCheckRequest(
    val userId: String,
    val deferred: CompletableDeferred<Boolean>
)