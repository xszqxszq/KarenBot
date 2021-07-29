package tk.xszq.otomadbot.core

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
open class Counter: ConcurrentHashMap<Long, Long>() {
    fun getRawId(subject: Contact) = (if (subject is Group) 1L else -1L) * subject.id
    fun increase(subject: Contact) = set(getRawId(subject), getOrDefault(getRawId(subject), 0) + 1)
    fun get(subject: Contact): Long = getOrDefault(getRawId(subject), 0)
    fun exceeded(subject: Contact, times: Long): Boolean = get(subject) >= times
    fun reset(subject: Contact) = set(getRawId(subject), 0)
}
class Cooldown: Counter() {
    fun update(subject: Contact) = set(getRawId(subject), System.currentTimeMillis())
    fun ready(subject: Contact, cooldown: Long) = get(subject) + cooldown <= System.currentTimeMillis()
}