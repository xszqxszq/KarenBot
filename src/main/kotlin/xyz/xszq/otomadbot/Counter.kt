package xyz.xszq.otomadbot

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import net.mamoe.mirai.event.events.MessageEvent
import xyz.xszq.OtomadBotCore

open class Counter: ConcurrentHashMap<Long, Long>() {
    fun getRawId(subject: Contact) = (if (subject is Group) 1L else -1L) * subject.id
    fun increase(subject: Contact) = set(getRawId(subject), getOrDefault(getRawId(subject), 0) + 1)
    fun get(subject: Contact): Long = getOrDefault(getRawId(subject), 0)
    fun exceeded(subject: Contact, times: Long): Boolean = get(subject) >= times
    fun reset(subject: Contact) = set(getRawId(subject), 0)
    fun reset(subject: Long) = set(subject, 0)
}
class Cooldown(val name: String): Counter() {
    fun update(subject: Contact) = set(getRawId(subject), System.currentTimeMillis())
    fun update(group: Long) = set(group, System.currentTimeMillis())
    fun set(subject: Contact, value: Long) = set(getRawId(subject), value)
    fun isReady(subject: Contact): Boolean = remaining(subject) < 0
    fun isReady(group: Long): Boolean = remaining(group) < 0
    fun remaining(subject: Contact) = get(subject) + getCooldown() - System.currentTimeMillis()
    fun remaining(group: Long) = getOrDefault(group, 0) + getCooldown() - System.currentTimeMillis()
    fun getCooldown(): Long {
        if (!CooldownConfig.data.values.containsKey(name)) {
            CooldownConfig.data.values[name]= 10000L
        }
        return CooldownConfig.data.values[name]!!
    }
}
class Quota(val name: String): Counter() {
    fun available(subject: Contact): Boolean = get(subject) < getQuota()
    fun getQuota(): Long {
        if (!QuotaConfig.data.values.containsKey(name)) {
            QuotaConfig.data.values[name] = 50L
        }
        return QuotaConfig.data.values[name]!!
    }
    fun update(subject: Contact) = increase(subject)
}

@Serializable
class MapValues(val values: MutableMap<String, Long>)
@Serializable
class MapStringValues(val values: MutableMap<String, String>)

object CooldownConfig: SafeYamlConfig<MapValues>(OtomadBotCore, "cooldown", MapValues(buildMap {
    put("accept", 1000L)
}.toMutableMap()))
object QuotaConfig: SafeYamlConfig<MapValues>(OtomadBotCore, "quota", MapValues(buildMap {
    put("image_template", 50L)
}.toMutableMap()))


suspend fun <T> MessageEvent.ifReady(cd: Cooldown, block: suspend () -> T): T? = if (cd.isReady(subject))
    block.invoke() else null
fun MessageEvent.update(cd: Cooldown) = cd.update(subject)
fun MessageEvent.available(q: Quota) = q.available(subject) ||
        (subject is Group && (subject as Group).members.size < 500)