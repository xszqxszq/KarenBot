package xyz.xszq.bot.dao

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ArcadeCenters: IntIdTable("arcade_center") {
    val group = reference("group_id", ArcadeQueueGroups)
    val name: Column<String> = text("name")
    val abbr: Column<String> = text("abbr")
    val value: Column<Int> = integer("value")
    val modified: Column<LocalDateTime> = datetime("modified").clientDefault {
        LocalDateTime.now()
    }
}