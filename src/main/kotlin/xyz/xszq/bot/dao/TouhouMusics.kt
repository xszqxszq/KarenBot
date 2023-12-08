package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object TouhouMusics : IdTable<String>("touhou_music") {
    override val id: Column<EntityID<String>> = varchar("id", 16).entityId()
    val name = varchar("name", 64)
    val version = varchar("version", 6)
    val filename = varchar("filename", 64)
}