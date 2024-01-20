package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ProberBinding(openId: EntityID<String>): Entity<String>(openId) {
    companion object : EntityClass<String, ProberBinding>(ProberBindings) {
        suspend fun queryBindings(openId: String): Pair<String, String>? = transactionWithLock {
            val bindings = findById(openId) ?: return@transactionWithLock null
            Pair(bindings.type, bindings.credential)
        }
    }
    var openId      by ProberBindings.id
    var type        by ProberBindings.type
    var credential  by ProberBindings.credential
}