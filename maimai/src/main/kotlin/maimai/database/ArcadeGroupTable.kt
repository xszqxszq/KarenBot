package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * 排队管理的机厅分组表
 */
object ArcadeGroupTable: IntIdTable() {
    val name = varchar("name", 32)
}