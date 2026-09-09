package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

/**
 * 排队管理的机厅分组
 *
 * 同一分组可多群共享
 */
class ArcadeGroup(id: EntityID<Int>) : IntEntity(id) {
    var name    by ArcadeGroupTable.name
    val arcades by Arcade referrersOn ArcadeTable.group

    /**
     * 组内查找指定名字/别名的机厅
     *
     * @param name 机厅名
     * @return 机厅
     */
    fun find(name: String) = arcades.firstOrNull { arcade ->
        arcade.matches(name)
    }

    companion object : IntEntityClass<ArcadeGroup>(ArcadeGroupTable) {
        /**
         * 根据名字查找分组
         *
         * @param name 分组名
         * @return 分组
         */
        suspend operator fun get(name: String): ArcadeGroup? = suspendedTransactionAsync {
            find { ArcadeGroupTable.name eq name }
        }.await().firstOrNull()
    }
}