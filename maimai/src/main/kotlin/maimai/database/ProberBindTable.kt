package xyz.xszq.bot.maimai.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import xyz.xszq.bot.database.newSuspendedTransaction
import xyz.xszq.bot.database.suspendedTransactionAsync

/**
 * 查分器绑定表
 */
@Suppress("unused")
object ProberBindTable: Table() {
    val id = varchar("id", 32)
    val prober = varchar("prober", 32)
    val key = varchar("key", 32)
    val value = text("value")
    override val primaryKey = PrimaryKey(id, prober, key)

    /**
     * 存入绑定
     * @param id 用户 OpenID
     * @param prober 查分器名
     * @param key 绑定键
     * @param value 绑定值
     */
    suspend operator fun set(
        id: String,
        prober: String,
        key: String,
        value: String
    ) = newSuspendedTransaction {
        if (selectAll().where {
                (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) and
                        (ProberBindTable.key eq key)
            }.count() != 0L)
            update({ (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) and
                    (ProberBindTable.key eq key) }) {
                it[ProberBindTable.value] = value
            }
        else
            insert {
                it[ProberBindTable.id] = id
                it[ProberBindTable.prober] = prober
                it[ProberBindTable.key] = key
                it[ProberBindTable.value] = value
            }
    }

    /**
     * 查询指定查分器的绑定
     *
     * @param id 用户 OpenID
     * @param prober 查分器名
     * @param key 绑定键
     * @return 绑定值
     */
    suspend operator fun get(
        id: String,
        prober: String,
        key: String
    ) = suspendedTransactionAsync {
        select(value).where {
            (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) and
                    (ProberBindTable.key eq key)
        }.map { it[value] }.firstOrNull() ?.let { it.ifBlank { null } }
    }.await()

    /**
     * 读取用户的全部绑定
     *
     * @param id 用户 OpenID
     */
    suspend operator fun get(
        id: String
    ) = suspendedTransactionAsync {
        selectAll().where {
            (ProberBindTable.id eq id)
        }
    }

    /**
     * 查询绑定了指定查分器与键的全部 OpenID
     *
     * @param prober 查分器名
     * @param key 绑定键
     * @return 用户 OpenID 列表
     */
    suspend fun idsForKey(
        prober: String,
        key: String
    ) = suspendedTransactionAsync {
        selectAll().where {
            (ProberBindTable.prober eq prober) and (ProberBindTable.key eq key)
        }.map { it[ProberBindTable.id] }.distinct()
    }.await()

    /**
     * 根据键反查用户 OpenID
     *
     * @param prober 查分器名
     * @param key 绑定键
     * @param bindValue 绑定值
     * @return 用户 OpenID
     */
    suspend fun findIdByValue(
        prober: String,
        key: String,
        bindValue: String
    ) = suspendedTransactionAsync {
        select(ProberBindTable.id).where {
            (ProberBindTable.prober eq prober) and (ProberBindTable.key eq key) and
                    (value eq bindValue)
        }.map { it[ProberBindTable.id] }.firstOrNull()
    }.await()

    /**
     * 删除用户的绑定
     *
     * @param id 用户 OpenID
     * @param prober 查分器
     */
    suspend fun delete(
        id: String,
        prober: String ?= null
    ) = newSuspendedTransaction {
        if (prober == null)
            deleteWhere { ProberBindTable.id eq id }
        else
            deleteWhere { (ProberBindTable.id eq id) and (ProberBindTable.prober eq prober) }
    }
}