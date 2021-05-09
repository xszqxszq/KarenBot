@file:Suppress("unused", "UNUSED_PARAMETER")
package tk.xszq.otomadbot.database

import com.github.houbb.opencc4j.util.ZhConverterUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import tk.xszq.otomadbot.*
import tk.xszq.otomadbot.api.getOCRText
import java.time.Instant

enum class ReplyRuleType(val type: Int) {
    INCLUDE(0),
    EQUAL(1),
    REGEX(2),
    ANY(3),
    ALL(4),
    PIC_INCLUDE(-1),
    PIC_ALL(-2),
    PIC_ANY(-3)
}

object ReplyRules : IntIdTable() {
    override val tableName = "reply"
    val name = varchar("name", 64)
    val rule = varchar("rule", 1024)
    val reply = varchar("reply", 1024)
    val group = long("qqgroup")
    val creator = long("creator")
    val type = integer("type")
    val createTime = timestamp("createtime")
    suspend fun getRulesBySubject(subject: Long): SizedIterable<ReplyRule> {
        return newSuspendedTransaction(db = Databases.mysql) { ReplyRule.find { group eq subject } }
    }
    suspend fun getRuleById(id: Int): ReplyRule? {
        return newSuspendedTransaction(db = Databases.mysql) { ReplyRule.findById(id) }
    }
    suspend fun insertRule(rule: String, reply: String, subject: Long, type: Int, creator: Long) {
        newSuspendedTransaction(db = Databases.mysql) {
            ReplyRule.new {
                this.name = ""
                this.rule = rule
                this.reply = reply
                this.group = subject
                this.type = type
                this.creator = creator
                this.createTime = Instant.now()
            }
        }
    }
    suspend fun removeRule(id: Int): Boolean {
        return newSuspendedTransaction(db = Databases.mysql) {
            var result = false
            ReplyRule.findById(id)?.let{
                it.delete()
                result = true
            }
            result
        }
    }
    private suspend fun matchInclude(event: GroupMessageEvent, content: String, ruleType: ReplyRuleType =
        ReplyRuleType.INCLUDE) = newSuspendedTransaction(db = Databases.mysql) {
        ReplyRule.find {
            type eq ruleType.type and
                    (group eq event.group.id or (group eq -1)) and InStr(content, rule)
        }.maxByOrNull { it.createTime }
    }
    private suspend fun matchEqual(event: GroupMessageEvent, content: String) =
        newSuspendedTransaction(db = Databases.mysql) {
        ReplyRule.find {
            type eq ReplyRuleType.EQUAL.type and
                    (group eq event.group.id or (group eq -1)) and (rule eq content)
        }.maxByOrNull { it.createTime }
    }
    private suspend fun matchRegex(event: GroupMessageEvent, content: String) =
        newSuspendedTransaction(db = Databases.mysql) {
        ReplyRule.find {
            type eq ReplyRuleType.REGEX.type and
                    (group eq event.group.id or (group eq -1)) and
                    RegexpOpCol(stringParam(content), "rule")
        }.maxByOrNull { it.createTime }
    }
    private suspend fun matchAny(event: GroupMessageEvent, content: String,
                                 ruleType: ReplyRuleType = ReplyRuleType.ANY) =
        newSuspendedTransaction(db = Databases.mysql) {
        var result: ReplyRule? = null
        ReplyRule.find {
            type eq ruleType.type and (group eq event.group.id)
        }.sortedByDescending { it.createTime }.forEach { rule ->
            rule.rule.split(",").forEach ensure@{ keyword ->
                if (keyword in content) {
                    result = rule; return@ensure
                }
            }
            result?.let { return@forEach }
        }
        result
    }
    private suspend fun matchAll(event: GroupMessageEvent, content: String,
                                 ruleType: ReplyRuleType = ReplyRuleType.ALL) =
        newSuspendedTransaction(db = Databases.mysql) {
        var result: ReplyRule? = null
        ReplyRule.find {
            type eq ruleType.type and (group eq event.group.id or (group eq -1))
        }.sortedByDescending { it.createTime }.forEach { rule ->
            var isMatched = rule.rule.isNotBlank()
            rule.rule.split(",").forEach ensure@{ keyword ->
                if (keyword !in content) {
                    isMatched = false; return@ensure
                }
            }
            if (isMatched) {
                result = rule
                return@forEach
            }
        }
        result
    }
    private suspend fun hasImageRule(group: Long): Boolean = newSuspendedTransaction(db = Databases.mysql) {
        ReplyRule.find { type less 0 and (ReplyRules.group eq group) }.count() > 0L
    }
    suspend fun match(event: GroupMessageEvent): String? = event.run {
        if (!message.anyIsInstance<PlainText>())
            return null
        var content = ZhConverterUtil.toSimple(event.message.content)!!
        var result: String? = null
        result ?: matchInclude(this, content)?.let { result = it.reply }
        result ?: matchEqual(this, content)?.let { result = it.reply }
        result ?: matchRegex(this, content)?.let { result = it.reply }
        result ?: matchAny(this, content)?.let { result = it.reply }
        result ?: matchAll(this, content)?.let { result = it.reply }
        result ?: run {
            if (message.anyIsInstance<Image>() && hasImageRule(group.id)) {
                content = ""
                message.forEach {
                    content += if (it is Image) getOCRText(it) + " " else ""
                }
                if (!content.isEmptyChar()) {
                    result ?: matchInclude(this, content, ReplyRuleType.PIC_INCLUDE)?.let { result = it.reply }
                    result ?: matchAll(this, content, ReplyRuleType.PIC_ALL)?.let { result = it.reply }
                    result ?: matchAny(this, content, ReplyRuleType.PIC_ANY)?.let { result = it.reply }
                }
            }
        }
        return result
    }
}
class ReplyRule(id: EntityID<Int>) : IntEntity(id) {
    var name by ReplyRules.name
    var rule by ReplyRules.rule
    var reply by ReplyRules.reply
    var group by ReplyRules.group
    var creator by ReplyRules.creator
    var type by ReplyRules.type
    var createTime by ReplyRules.createTime

    companion object : IntEntityClass<ReplyRule>(ReplyRules) {
        fun getNameFromType(type: Int): String? {
            return when (type) {
                ReplyRuleType.PIC_INCLUDE.type -> "图片包含"
                ReplyRuleType.PIC_ALL.type -> "图片全含"
                ReplyRuleType.PIC_ANY.type -> "图片任含"
                ReplyRuleType.INCLUDE.type -> "包含"
                ReplyRuleType.EQUAL.type -> "全等"
                ReplyRuleType.REGEX.type -> "正则"
                ReplyRuleType.ANY.type -> "任含"
                ReplyRuleType.ALL.type -> "全含"
                else -> null
            }
        }
        fun getTypeFromName(name: String): Int? {
            return when (name) {
                "图片包含" -> ReplyRuleType.PIC_INCLUDE.type
                "图片全含" -> ReplyRuleType.PIC_ALL.type
                "图片任含" -> ReplyRuleType.PIC_ANY.type
                "包含" -> ReplyRuleType.INCLUDE.type
                "全等" -> ReplyRuleType.EQUAL.type
                "正则" -> ReplyRuleType.REGEX.type
                "任含" -> ReplyRuleType.ANY.type
                "全含" -> ReplyRuleType.ALL.type
                else -> null
            }
        }
    }
}