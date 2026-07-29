package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.id.IdTable

object ArcadeGroupBindTable: IdTable<String>() {
    override val id = varchar("id", 32).entityId()
    val group = reference("group", ArcadeGroupTable)
}