package xyz.xszq.bot.dao

import org.jetbrains.exposed.sql.Table

object TouhouAliases: Table("touhou_aliases") {
    val id = varchar("id", 16)
    val alias = varchar("alias", 512)
}