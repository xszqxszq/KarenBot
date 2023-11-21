package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.id.IntIdTable

object ArcadeQueueGroups: IntIdTable("arcade_queue_group") {
    val name = varchar("name", 64)
}