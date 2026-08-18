package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

class ArcadeGroup(id: EntityID<Int>) : IntEntity(id) {
    var name    by ArcadeGroupTable.name
    val arcades by Arcade referrersOn ArcadeTable.group

    fun find(name: String) = arcades.firstOrNull { arcade ->
        arcade.matches(name)
    }

    companion object : IntEntityClass<ArcadeGroup>(ArcadeGroupTable) {
        suspend operator fun get(name: String): ArcadeGroup? = suspendedTransactionAsync {
            find { ArcadeGroupTable.name eq name }
        }.await().firstOrNull()
    }
}