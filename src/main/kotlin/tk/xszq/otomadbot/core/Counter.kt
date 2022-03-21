package tk.xszq.otomadbot.core

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.MessageEvent
import tk.xszq.otomadbot.api.GOCQGuildMessageEvent
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
open class Counter: ConcurrentHashMap<Long, Long>() {
    fun getRawId(subject: Contact) = (if (subject is Group) 1L else -1L) * subject.id
    fun increase(subject: Contact) = set(getRawId(subject), getOrDefault(getRawId(subject), 0) + 1)
    fun get(subject: Contact): Long = getOrDefault(getRawId(subject), 0)
    fun exceeded(subject: Contact, times: Long): Boolean = get(subject) >= times
    fun reset(subject: Contact) = set(getRawId(subject), 0)
    fun reset(subject: Long) = set(subject, 0)
}
@Suppress("MemberVisibilityCanBePrivate")
class Cooldown(val name: String): Counter() {
    fun update(subject: Contact) = set(getRawId(subject), System.currentTimeMillis())
    fun update(group: Long) = set(group, System.currentTimeMillis())
    fun set(subject: Contact, value: Long) = set(getRawId(subject), value)
    fun isReady(subject: Contact): Boolean = remaining(subject) < 0
    fun isReady(group: Long): Boolean = remaining(group) < 0
    fun remaining(subject: Contact) = get(subject) + getCooldown() - System.currentTimeMillis()
    fun remaining(group: Long) = getOrDefault(group, 0) + getCooldown() - System.currentTimeMillis()
    fun getCooldown(): Long {
        if (!CooldownConfig.cooldown.containsKey(name)) {
            CooldownConfig.edit(name, 10000L)
        }
        return CooldownConfig.cooldown[name]!!
    }
}
suspend fun <T> MessageEvent.ifReady(cd: Cooldown, block: suspend () -> T): T? = if (cd.isReady(subject))
    block.invoke() else null
suspend fun <T> GOCQGuildMessageEvent.ifReady(cd: Cooldown, block: suspend () -> T): T? =
    if (cd.isReady(channel.id.filter { it.isDigit() }.toLong()))
    block.invoke() else null
fun MessageEvent.update(cd: Cooldown) = cd.update(subject)
fun MessageEvent.remaining(cd: Cooldown) = cd.remaining(subject) / 1000L
fun GOCQGuildMessageEvent.update(cd: Cooldown) = cd.update(channel.id.filter { it.isDigit() }.toLong())
fun GOCQGuildMessageEvent.remaining(cd: Cooldown) = cd.remaining(channel.id.filter { it.isDigit() }.toLong()) / 1000L

object CooldownConfig: AutoSavePluginConfig("cooldown") {
    var cooldown: Map<String, Long> by value()
    fun edit(key: String, value: Long) {
        val temp = cooldown.toMutableMap()
        temp[key] = value
        cooldown = temp
    }
}