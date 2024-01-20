package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ProberBindings : IdTable<String>("maimai_binding") {
    override val id: Column<EntityID<String>> = varchar("openid", 32).entityId()
    val type: Column<String> = varchar("type", 8)
    val credential: Column<String> = varchar("credential", 64)
}