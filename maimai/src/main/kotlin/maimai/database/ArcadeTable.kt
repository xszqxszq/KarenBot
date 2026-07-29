package xyz.xszq.bot.maimai.database

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ArcadeTable: IntIdTable() {
    val group = reference("group", ArcadeGroupTable)
    val name = varchar("name", 32)
    val aliases = text("aliases")
    val value = integer("value")
    val modified = datetime("modified").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
}