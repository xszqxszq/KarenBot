package xyz.xszq.bot.text

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.selectAll
import xyz.xszq.bot.dao.QARuleDAO
import xyz.xszq.bot.dao.QARules
import xyz.xszq.bot.dao.transactionWithLock
import xyz.xszq.nereides.event.PublicMessageEvent
import java.time.LocalDateTime

class QARule(
    var openid: String,
    var type: Int,
    var rule: String,
    var reply: String,
    var creator: String,
    var createTime: LocalDateTime
)
object AutoQA {
    private var rules = listOf<QARule>()
    private val mutex = Mutex()
    private var cacheTime = 0L
    private suspend fun fetchFromDB() = transactionWithLock {
        QARules.selectAll().map { QARule(
            it[QARules.openid],
            it[QARules.type],
            it[QARules.rule],
            it[QARules.reply],
            it[QARules.creator],
            it[QARules.createTime],
        ) }
    }
    private suspend fun update(force: Boolean = false) {
        if (force || System.currentTimeMillis() - cacheTime >= 60 * 1000L) {
            val data = fetchFromDB()
            mutex.withLock {
                rules = data
                cacheTime = System.currentTimeMillis()
            }
        }
    }
    private suspend fun matchText(msg: String, contextId: String): String? {
        update()
        mutex.withLock {
            rules.filter { it.openid == "-1" || it.openid == contextId }
        }.forEach { rule ->
            val matched = when (rule.type) {
                QARuleDAO.INCLUDE -> rule.rule.lowercase() in msg
                QARuleDAO.EQUAL -> rule.rule.lowercase() == msg
                QARuleDAO.REGEX -> Regex(rule.rule.lowercase()).find(msg) != null
                QARuleDAO.ANY -> {
                    var matched = false
                    rule.rule.lowercase().split(",").forEach { if (it in msg) { matched = true } }
                    matched
                }
                QARuleDAO.ALL -> {
                    var matched = true
                    val keywords = rule.rule.lowercase().split(",")
                    keywords.forEach { if (it !in msg) { matched = false } }
                    keywords.isNotEmpty() && matched

                }
                else -> false
            }
            if (matched)
                return rule.reply
        }
        return null
    }
    suspend fun handle(event: PublicMessageEvent) = event.run {
        matchText(message.text, contextId) ?.let {
            reply(it)
        }
    }
}