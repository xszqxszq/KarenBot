package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.id.IdTable

/**
 * 排队管理的机厅分组绑定表
 */
@Suppress("unused")
object ArcadeGroupBindTable: IdTable<String>() {
    override val id = varchar("id", 32).entityId()
    val group = reference("group", ArcadeGroupTable)
}