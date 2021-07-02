@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package tk.xszq.otomadbot.core.text

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.anyIsInstance
import net.mamoe.mirai.message.data.content
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import tk.xszq.otomadbot.core.*
import tk.xszq.otomadbot.core.api.PaddleOCR
import tk.xszq.otomadbot.core.text.AutoReplyHandler.database
import tk.xszq.otomadbot.core.text.AutoReplyHandler.newLockedSuspendedTransaction
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
class AutoReplyRule(val id: Int,
                    val name: String = "",
                    val rule: String,
                    val type: Byte = 0,
                    val group: Long,
                    val reply: String,
                    val creator: Long,
                    val createTime: Long)
@Serializable
class AutoReplyRules {
    var nextId = 1
    val rules = mutableMapOf<Int, AutoReplyRule>()
}

object RuleItems: IntIdTable() {
    override val tableName = "reply"
    val name = varchar("name", 50)
    val rule = text("rule")
    val type = byte("type")
    val group = long("qqgroup")
    val reply = varchar("reply", 1024)
    val creator = long("creator")
    val createTime = long("createtime")
    suspend fun getRulesBySubject(subject: Long): SizedIterable<RuleItem> {
        return newLockedSuspendedTransaction(database) { RuleItem.find { group eq subject } }
    }
    suspend fun getRuleById(id: Int): RuleItem? {
        return newLockedSuspendedTransaction(database) { RuleItem.findById(id) }
    }
    suspend fun insertRule(name: String, rule: String, reply: String, group: Long, type: Byte, creator: Long,
        createTime: Long = System.currentTimeMillis()/1000) {
        AutoReplyHandler.config.rules[AutoReplyHandler.config.nextId] =
            AutoReplyRule(AutoReplyHandler.config.nextId, name, rule, type, group, reply, creator, createTime)
        newLockedSuspendedTransaction(database) {
            RuleItems.insert { ruleItem ->
                ruleItem[RuleItems.id] = AutoReplyHandler.config.nextId
                ruleItem[RuleItems.name] = name
                ruleItem[RuleItems.rule] = rule
                ruleItem[RuleItems.type] = type
                ruleItem[RuleItems.group] = group
                ruleItem[RuleItems.reply] = reply
                ruleItem[RuleItems.creator] = creator
                ruleItem[RuleItems.createTime] = createTime
            }
        }
        AutoReplyHandler.config.nextId += 1
        AutoReplyHandler.saveConfig()
    }
    suspend fun removeRule(id: Int): Boolean {
        return try {
            AutoReplyHandler.config.rules.remove(id)
            newLockedSuspendedTransaction(database) {
                RuleItems.deleteWhere { RuleItems.id eq id }
            }
            AutoReplyHandler.saveConfig()
            true
        } catch (e: Exception) {
            false
        }
    }
    private suspend fun matchInclude(event: GroupMessageEvent, content: String, ruleType: ReplyRuleType =
        ReplyRuleType.INCLUDE) = newLockedSuspendedTransaction(database) {
        RuleItem.find {
            type eq ruleType.type and
                    (group eq event.group.id or (group eq -1)) and InStr(content, rule)
        }.maxByOrNull { it.createTime }
    }
    private suspend fun matchEqual(event: GroupMessageEvent, content: String) =
        newLockedSuspendedTransaction(database) {
            RuleItem.find {
                type eq ReplyRuleType.EQUAL.type and
                        (group eq event.group.id or (group eq -1)) and (rule eq content)
            }.maxByOrNull { it.createTime }
        }
    private suspend fun matchRegex(event: GroupMessageEvent, content: String) =
        newLockedSuspendedTransaction(database) {
            RuleItem.find {
                type eq ReplyRuleType.REGEX.type and
                        (group eq event.group.id or (group eq -1)) and
                        RegexpOpCol(stringParam(content), "rule")
            }.maxByOrNull { it.createTime }
        }
    private suspend fun matchAny(event: GroupMessageEvent, content: String,
                                 ruleType: ReplyRuleType = ReplyRuleType.ANY) =
        newLockedSuspendedTransaction(database) {
            var result: RuleItem? = null
            RuleItem.find {
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
        newLockedSuspendedTransaction(database) {
            var result: RuleItem? = null
            RuleItem.find {
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
    private suspend fun hasImageRule(group: Long): Boolean = newLockedSuspendedTransaction(database) {
        RuleItem.find { type less 0 and (RuleItems.group eq group) }.count() > 0L
    }
    suspend fun match(event: GroupMessageEvent): String? = event.run {
        var content = message.content.toSimple()
        var result: String? = null
        if (message.anyIsInstance<PlainText>()) {
            result ?: matchInclude(this, content)?.let { result = it.reply }
            result ?: matchEqual(this, content)?.let { result = it.reply }
            result ?: matchRegex(this, content)?.let { result = it.reply }
            result ?: matchAny(this, content)?.let { result = it.reply }
            result ?: matchAll(this, content)?.let { result = it.reply }
        }
        result ?: run {
            if (message.anyIsInstance<Image>() && hasImageRule(group.id)) {
                content = ""
                message.forEach {
                    content += if (it is Image) PaddleOCR.getText(it) + " " else ""
                }
                println(content)
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
class RuleItem(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RuleItem>(RuleItems) {
        fun getNameFromType(type: Byte): String? {
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
        fun getTypeFromName(name: String): Byte? {
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
    var name by RuleItems.name
    var rule by RuleItems.rule
    var type by RuleItems.type
    var group by RuleItems.group
    var reply by RuleItems.reply
    var creator by RuleItems.creator
    var createTime by RuleItems.createTime
}
enum class ReplyRuleType(val type: Byte) {
    INCLUDE(0),
    EQUAL(1),
    REGEX(2),
    ANY(3),
    ALL(4),
    PIC_INCLUDE(-1),
    PIC_ALL(-2),
    PIC_ANY(-3)
}
object AutoReplyHandler {
    val database = Database.connect("jdbc:h2:mem:reply;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
    val rwLock = AtomicBoolean(false)
    var config = AutoReplyRules()
    suspend fun <T> newLockedSuspendedTransaction(db: Database = database, statement: suspend Transaction.() -> T): T {
        while (rwLock.get()) delay(2000)
        return newSuspendedTransaction(db = db) {
            statement.invoke(this)
        }
    }
    fun saveConfig() {
        OtomadBotCore.configFolderPath.resolve("reply.yml").toFile()
            .writeText(Yaml.default.encodeToString(AutoReplyRules.serializer(), config))
    }
    suspend fun reloadConfig() {
        config = Yaml.default.decodeFromString(AutoReplyRules.serializer(), OtomadBotCore.configFolderPath
            .resolve("reply.yml").toFile().readText())
        newLockedSuspendedTransaction(database) {
            SchemaUtils.create(RuleItems)
            RuleItems.deleteAll()
            config.rules.forEach { (_, confItem) ->
                RuleItems.insert { ruleItem ->
                    ruleItem[id] = confItem.id
                    ruleItem[name] = confItem.name
                    ruleItem[rule] = confItem.rule
                    ruleItem[type] = confItem.type
                    ruleItem[group] = confItem.group
                    ruleItem[reply] = confItem.reply
                    ruleItem[creator] = confItem.creator
                    ruleItem[createTime] = confItem.createTime
                }
            }
        }
    }
    fun register() {
        rwLock.set(false)
        runBlocking {
            reloadConfig()
        }
        GlobalEventChannel.subscribeAlways<GroupMessageEvent> {

            RuleItems.match(this) ?.let {
                quoteReply(it)
            }
        }
    }
}