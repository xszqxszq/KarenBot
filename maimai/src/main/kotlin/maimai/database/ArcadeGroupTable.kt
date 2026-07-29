package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.id.IntIdTable

object ArcadeGroupTable: IntIdTable() {
    val name = varchar("name", 32)
}