package xyz.xszq.bot.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

class ArcadeGroup(id: EntityID<Int>) : IntEntity(id) {
    var name    by ArcadeGroupTable.name
    val arcades by Arcade referrersOn ArcadeTable.group
    companion object : IntEntityClass<ArcadeGroup>(ArcadeGroupTable) {
        suspend operator fun get(name: String): ArcadeGroup? = suspendedTransactionAsync {
            find { ArcadeGroupTable.name eq name }
        }.await().firstOrNull()
    }
    suspend fun find(name: String) = suspendedTransactionAsync {
        arcades.firstOrNull { arcade ->
            arcade.name.lowercase() == name.lowercase() ||
                name.lowercase() in arcade.aliases.split(",").map { it.lowercase() } }
    }.await()
}