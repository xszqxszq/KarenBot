package xyz.xszq.bot.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

class Arcade(id: EntityID<Int>): IntEntity(id) {
    var group       by ArcadeTable.group
    var name        by ArcadeTable.name
    var aliases        by ArcadeTable.aliases
    var value       by ArcadeTable.value
    var modified    by ArcadeTable.modified

    suspend fun clear() = suspendedTransactionAsync {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (modified == initTime || now.isSameDay(modified))
            return@suspendedTransactionAsync
        value = 0
        modified = initTime
    }
    fun noUpdates() = modified == initTime

    companion object : IntEntityClass<Arcade>(ArcadeTable) {
        private val initTime = LocalDateTime(2000, 1, 1, 0, 0)
        suspend fun new(group: ArcadeGroup, name: String) = suspendedTransactionAsync {
            new {
                this.group = group.id
                this.name = name
                this.aliases = name
                this.value = 0
            }
        }.await()
        fun LocalDateTime.isSameDay(b: LocalDateTime): Boolean =
            year == b.year && month == b.month && dayOfMonth == b.dayOfMonth
    }
}