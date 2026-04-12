package xyz.xszq.bot.maimai.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class Arcade(id: EntityID<Int>): IntEntity(id) {
    var group       by ArcadeTable.group
    var name        by ArcadeTable.name
    var aliases        by ArcadeTable.aliases
    var value       by ArcadeTable.value
    var modified    by ArcadeTable.modified

    data class Snapshot(
        val name: String,
        val aliases: List<String>,
        val value: Int,
        val modified: LocalDateTime,
    ) {
        fun noUpdates() = modified == initTime
    }

    sealed interface UpdateResult {
        data class Updated(val arcade: Snapshot): UpdateResult
        data object TooLarge: UpdateResult
    }

    suspend fun clear() = newSuspendedTransaction {
        clearInTransaction()
    }

    fun clearInTransaction(currentTime: LocalDateTime = currentTime()) {
        if (modified == initTime || currentTime.isSameDay(modified))
            return
        value = 0
        modified = initTime
    }

    fun matches(name: String) =
        this.name.equals(name, ignoreCase = true) ||
            aliases.split(",").filter { it.isNotBlank() }.any { it.equals(name, ignoreCase = true) }

    fun snapshot() = Snapshot(
        name = name,
        aliases = aliases.split(",").filter { it.isNotBlank() },
        value = value,
        modified = modified,
    )

    fun noUpdates() = modified == initTime

    companion object : IntEntityClass<Arcade>(ArcadeTable) {
        internal val initTime = LocalDateTime(2000, 1, 1, 0, 0)

        private fun currentTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        suspend fun new(group: ArcadeGroup, name: String) = newSuspendedTransaction {
            new {
                this.group = group.id
                this.name = name
                this.aliases = name
                this.value = 0
            }
        }

        suspend fun clearAll() = newSuspendedTransaction {
            all().forEach {
                it.clearInTransaction()
            }
        }

        fun LocalDateTime.isSameDay(b: LocalDateTime): Boolean =
            year == b.year && month == b.month && dayOfMonth == b.dayOfMonth
    }
}
