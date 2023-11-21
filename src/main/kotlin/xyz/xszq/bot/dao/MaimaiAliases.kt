package xyz.xszq.bot.dao

import org.jetbrains.exposed.sql.Table

object MaimaiAliases: Table("maimai_aliases") {
    val id = varchar("id", 16)
    val name = varchar("name", 512)
}