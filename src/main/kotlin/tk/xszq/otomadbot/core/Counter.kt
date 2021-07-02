package tk.xszq.otomadbot.core

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class Counter: ConcurrentHashMap<Long, Long>() {
    fun increase(subject: Contact) {
        if (subject is Group)
            set(subject.id, getOrDefault(subject.id, 0) + 1)
        else
            set(-subject.id, getOrDefault(-subject.id, 0) + 1)
    }
    fun get(subject: Contact): Long =
        getOrDefault((if (subject is Group) 1L else -1L) * subject.id, 0)
    fun exceeded(subject: Contact, times: Long): Boolean = get(subject) >= times
    fun reset(subject: Contact) = set((if (subject is Group) 1L else -1L) * subject.id, 0)
}