package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.update

/**
 * 水鱼查分器更新 Token 绑定表
 *
 * 保存用户的水鱼更新 Token，用于更新水鱼查分器
 */
object DivingFishBindTable: Table() {
    val id = varchar("id", 32)
    val importToken = text("importToken")
    override val primaryKey = PrimaryKey(id)

    /**
     * 修改用户的更新 Token
     *
     * @param openId 用户 OpenID
     * @param importToken 水鱼更新 Token
     */
    suspend fun update(openId: String, importToken: String) = newSuspendedTransaction {
        if (selectAll().where {
                DivingFishBindTable.id eq openId
            }.count() != 0L)
            update({ DivingFishBindTable.id eq openId }) {
                it[DivingFishBindTable.importToken] = importToken
            }
        else
            insert {
                it[id] = openId
                it[DivingFishBindTable.importToken] = importToken
            }
    }

    /**
     * 读取用户绑定的更新 Token
     *
     * @param openId 用户 OpenID
     * @return 水鱼更新 Token
     */
    suspend operator fun get(openId: String) = suspendedTransactionAsync {
        select(importToken).where {
            DivingFishBindTable.id eq openId
        }.map { it[importToken] }.singleOrNull()
    }.await()
}