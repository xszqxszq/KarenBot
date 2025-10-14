package xyz.xszq.bot.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

class ArcadeGroupBind(id: EntityID<String>): Entity<String>(id) {
    var group by ArcadeGroupBindTable.group
    companion object : EntityClass<String, ArcadeGroupBind>(ArcadeGroupBindTable) {
        suspend fun group(
            openId: String
        ) = suspendedTransactionAsync {
            findById(openId) ?.let {
                ArcadeGroup.findById(it.group)
            } ?: ArcadeGroup.new {
                name = openId
            }.also { newGroup ->
                new(openId) { group = newGroup.id }
            }
        }.await()
        suspend fun find(
            openId: String
        ) = suspendedTransactionAsync {
            findById(openId) ?.let {
                ArcadeGroup.findById(it.group)
            }
        }.await()
        suspend fun bind(openId: String, group: ArcadeGroup) = suspendedTransactionAsync {
            findById(openId) ?.let {
                it.group = group.id
            } ?: new(openId) {
                this.group = group.id
            }
        }.await()
    }
}