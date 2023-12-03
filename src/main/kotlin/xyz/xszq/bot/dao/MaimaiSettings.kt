package xyz.xszq.bot.dao

import org.jetbrains.exposed.sql.Table

object MaimaiSettings: Table("maimai_settings") {
    val openid = varchar("openid", 32)
    val name = varchar("name", 32)
    val value = varchar("value", 32)
}