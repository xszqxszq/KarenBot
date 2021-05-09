@file:Suppress("UNUSED", "MemberVisibilityCanBePrivate")
package tk.xszq.otomadbot.database

import com.soywiz.korio.async.launchImmediately
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import tk.xszq.otomadbot.configMain
import java.util.concurrent.atomic.AtomicBoolean

object Databases {
    val mysql by lazy {
        Database.connect(
            "jdbc:mysql://${configMain.database.host}/${configMain.database.database}${configMain.database.args}",
            "com.mysql.jdbc.Driver",
            user = configMain.database.username,
            password = configMain.database.password
        )
    }
    val cache by lazy {
        Database.connect("jdbc:h2:mem:cache;MODE=MySQL;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
    }
    var refreshLock = AtomicBoolean()
    suspend fun init() {
        refreshLock.set(false)
        newSuspendedTransaction(db = cache) {
            SchemaUtils.create(Counters)
            SchemaUtils.create(Permissions)
            SchemaUtils.create(ReplyRules)
        }
        TasksExecutor.start()
    }
    suspend fun refreshCache() {
        try {
            val perms = newSuspendedTransaction(db = mysql) {
                Permission.all().toList()
            }
            val rules = newSuspendedTransaction(db = mysql) {
                ReplyRule.all().toList()
            }
            newSuspendedTransaction(db = cache) {
                refreshLock.set(true)
                Permissions.deleteAll()
                Permissions.batchInsert(perms) {
                    this[Permissions.id] = it.id
                    this[Permissions.name] = it.name
                    this[Permissions.enabled] = it.enabled
                    this[Permissions.subject] = it.subject
                }
                ReplyRules.deleteAll()
                ReplyRules.batchInsert(rules) {
                    this[ReplyRules.createTime] = it.createTime
                    this[ReplyRules.creator] = it.creator
                    this[ReplyRules.group] = it.group
                    this[ReplyRules.name] = it.name
                    this[ReplyRules.reply] = it.reply
                    this[ReplyRules.rule] = it.rule
                    this[ReplyRules.type] = it.type
                    this[ReplyRules.id] = it.id
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            refreshLock.set(false)
        }
    }
    object TasksExecutor {
        fun start() {
            GlobalScope.launchImmediately {
                while (true) {
                    if (!refreshLock.get())
                        refreshCache()
                    delay(60000)
                }
            }
        }
    }
}
suspend fun <T> Transaction.acquireLock(statement: Transaction.() -> T): T {
    while (Databases.refreshLock.get()) delay(2000)
    return statement.invoke(this)
}
suspend fun <T> newLockedSuspendedTransaction(db: Database = Databases.cache, statement: Transaction.() -> T): T {
    while (Databases.refreshLock.get()) delay(2000)
    return newSuspendedTransaction(db = db) {
        statement.invoke(this)
    }
}

/**
 * Define "WHERE string REGEXP `column`" query.
 */
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