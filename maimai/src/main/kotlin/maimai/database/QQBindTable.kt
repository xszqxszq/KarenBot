package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.update

/**
 * QQ 绑定表
 *
 * 目前用户绑定已全面转向 OAuth，QQ 绑定将于未来废除
 */
@Suppress("unused")
object QQBindTable: Table() {
    val id = varchar("id", 32)
    val qq = long("qq")
    override val primaryKey = PrimaryKey(id)

    /**
     * 写入或更新用户的 QQ 绑定
     *
     * @param openId 用户 OpenID
     * @param qq QQ
     */
    suspend fun update(openId: String, qq: Long) = newSuspendedTransaction {
        if (selectAll().where {
                QQBindTable.id eq openId
            }.count() != 0L)
            update({ QQBindTable.id eq openId }) {
                it[QQBindTable.qq] = qq
            }
        else
            insert {
                it[id] = openId
                it[QQBindTable.qq] = qq
            }
    }

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

    /**
     * 读取全部绑定记录
     *
     * @return 查询结果
     */
    suspend fun allBindings(): List<Pair<String, Long>> = suspendedTransactionAsync {
        selectAll().map { row ->
            Pair(row[QQBindTable.id], row[qq])
        }
    }.await()
}