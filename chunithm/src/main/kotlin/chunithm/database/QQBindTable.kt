package xyz.xszq.bot.chunithm.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync

/**
 * QQ 绑定表
 *
 * 目前用户绑定已全面转向 OAuth，QQ 绑定将于未来废除
 */
object QQBindTable: Table() {
    val id = varchar("id", 32)
    val qq = long("qq")
    override val primaryKey = PrimaryKey(id)

    /**
     * 读取用户绑定的 QQ
     *
     * @param openId 用户 OpenID
     * @return QQ
     */
    suspend operator fun get(openId: String) = suspendedTransactionAsync {
        select(qq).where {
            QQBindTable.id eq openId
        }.map { it[qq] }.firstOrNull()
    }.await()
}