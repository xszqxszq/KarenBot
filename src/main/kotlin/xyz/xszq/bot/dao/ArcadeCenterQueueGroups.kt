package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object ArcadeCenterQueueGroups: IdTable<String>("arcade_center_queue_group") {
    override val id: Column<EntityID<String>> = varchar("openid", 32).entityId()
    val group = reference("group_id", ArcadeQueueGroups)
}