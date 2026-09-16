package xyz.xszq.bot.maimai.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.update
import xyz.xszq.bot.database.newSuspendedTransaction

/**
 * 排队管理的机厅
 */
class Arcade(id: EntityID<Int>): IntEntity(id) {
    var group       by ArcadeTable.group
    var name        by ArcadeTable.name
    var aliases        by ArcadeTable.aliases
    var value       by ArcadeTable.value
    var modified    by ArcadeTable.modified

    /**
     * 判断机厅今日是否尚未更新过排卡数据
     */
    fun noUpdates() = modified == initTime

    /**
     * 清空隔天未更新的机厅数据
     *
     * @param currentTime 当前时间
     */
    fun clear(currentTime: LocalDateTime = currentTime()) {
        if (modified == initTime || currentTime.isSameDay(modified))
            return
        ArcadeTable.update({ ArcadeTable.id eq id }) {
            it[ArcadeTable.value] = 0
            it[ArcadeTable.modified] = initTime
        }
    }

    /**
     * 判断机厅名是否匹配到名称/别名
     *
     * @param name 机厅名
     */
    fun matches(name: String) =
        this.name.equals(name, ignoreCase = true) ||
            aliases.split(",").filter { it.isNotBlank() }.any { it.equals(name, ignoreCase = true) }

    companion object : IntEntityClass<Arcade>(ArcadeTable) {
        internal val initTime = LocalDateTime(2000, 1, 1, 0, 0)

        private fun currentTime() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        /**
         * 在指定分组下新建机厅
         *
         * @param group 分组
         * @param name 机厅名
         */
        suspend fun new(group: ArcadeGroup, name: String) = newSuspendedTransaction {
            new {
                this.group = group.id
                this.name = name
                this.aliases = name
                this.value = 0
            }
        }

        /**
         * 清除隔天未更新的机厅数据
         */
        suspend fun clearAll() = newSuspendedTransaction {
            all().forEach {
                it.clear()
            }
        }

        private fun LocalDateTime.isSameDay(b: LocalDateTime): Boolean =
            year == b.year && month == b.month && dayOfMonth == b.dayOfMonth
    }
}