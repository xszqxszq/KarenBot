@file:Suppress("unused")
package tk.xszq.otomadbot.database

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import tk.xszq.otomadbot.configMain

object Counters : Table() {
    override val tableName = "counter"
    val param = varchar("param", 32)
    val times = long("times")
    val subject = long("subject")
}
open class Counter(val name: String, private val group: Long) {
    suspend fun value() = newSuspendedTransaction(db = Databases.cache) {
        Counters.select { Counters.param eq name and (Counters.subject eq group) }.first()[Counters.times]
    }
    suspend fun set(value: Long) = newSuspendedTransaction(db = Databases.cache) {
        Counters.update({ Counters.param eq name and (Counters.subject eq group) }) {
            it[times] = value
        }
    }
    suspend fun increase() = newSuspendedTransaction(db = Databases.cache) {
        Counters.update({ Counters.param eq name and (Counters.subject eq group) }) {
            with(SqlExpressionBuilder) { it.update(times, times + 1) }
        }
    }
    suspend fun decrease() = newSuspendedTransaction(db = Databases.cache) {
        Counters.update({ Counters.param eq name and (Counters.subject eq group) }) {
            with(SqlExpressionBuilder) { it.update(times, times - 1) }
        }
    }
    init {
        transaction(db = Databases.cache) {
            if (Counters.select{ Counters.param eq name and (Counters.subject eq group) }.count() == 0L) {
                Counters.insert{ it[param] = name; it[subject] = group; it[times] = 0 }
            }
        }
    }
}
class CooldownCounter(name: String, group: Long): Counter(name, group) {
    @SuppressWarnings("WeakerAccess")
    suspend fun isReady(): Boolean = remaining() >= configMain.cooldown[name]?.toLong()!!
    suspend fun isNotReady(): Boolean = !isReady()
    suspend fun update() {
        set(System.currentTimeMillis())
    }
    suspend fun remaining() = System.currentTimeMillis() - value()
    suspend fun <T> onReady(block: suspend (CooldownCounter) -> T): T? = if (isReady()) block(this) else null
}
fun GroupMessageEvent.getCounter(name: String) = Counter(name, group.id)
fun GroupMessageEvent.getCooldown(name: String) = CooldownCounter(name, group.id)