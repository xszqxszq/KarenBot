package xyz.xszq.bot

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import xyz.xszq.bot.dao.QARule
import xyz.xszq.bot.dao.QARules
import xyz.xszq.nereides.event.GroupAtMessageEvent

object AutoQA {
    private suspend fun matchText(msg: String, openId: String): String? = suspendedTransactionAsync(Dispatchers.IO) {
        QARules.select {
            (QARules.openid eq "-1") or (QARules.openid eq openId)
        }.forEach { rule ->
            val matched = when (rule[QARules.type]) {
                QARule.INCLUDE -> rule[QARules.rule].lowercase() in msg
                QARule.EQUAL -> rule[QARules.rule].lowercase() == msg
                QARule.REGEX -> Regex(rule[QARules.rule].lowercase()).find(msg) != null
                QARule.ANY -> {
                    var matched = false
                    rule[QARules.rule].lowercase().split(",").forEach { if (it in msg) { matched = true } }
                    matched
                }
                QARule.ALL -> {
                    var matched = true
                    val keywords = rule[QARules.rule].lowercase().split(",")
                    keywords.forEach { if (it !in msg) { matched = false } }
                    keywords.isNotEmpty() && matched

                }
                else -> false
            }
            if (matched)
                return@suspendedTransactionAsync rule[QARules.reply]
        }
        return@suspendedTransactionAsync null
    }.await()
    suspend fun handle(event: GroupAtMessageEvent) = event.run {
        matchText(content, groupId) ?.let {
            reply(it)
        }
    }
}