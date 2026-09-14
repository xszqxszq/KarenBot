package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * 群成员信息表
 */
object GroupMemberTable: Table() {
    /**
     * 群 OpenID
     */
    val group = varchar("group_id", 32)
    /**
     * 成员 OpenID
     */
    val member = varchar("member_id", 32)
    override val primaryKey = PrimaryKey(group, member)

    init {
        index("group_members_member", false, member)
    }

    /**
     * 遍历全部群成员关系
     *
     * @param db 数据库连接
     * @param block 处理逻辑
     */
    suspend fun forEach(
        db: Database,
        block: (String, String) -> Unit
    ) = newSuspendedTransaction(db = db) {
        selectAll().forEach { row ->
            block(row[group], row[member])
        }
    }

    /**
     * 存入群成员记录
     *
     * @param db 数据库连接
     * @param group 群 OpenID
     * @param member 成员 OpenID
     */
    suspend fun add(
        db: Database,
        group: String,
        member: String
    ) = newSuspendedTransaction(db = db) {
        val match = (GroupMemberTable.group eq group) and
            (GroupMemberTable.member eq member)
        if (selectAll().where { match }.count() == 0L)
            insert {
                it[GroupMemberTable.group] = group
                it[GroupMemberTable.member] = member
            }
    }

    /**
     * 删除群成员记录
     *
     * @param db 数据库连接
     * @param group 群 OpenID
     * @param member 成员 OpenID
     */
    suspend fun remove(
        db: Database,
        group: String,
        member: String
    ) = newSuspendedTransaction(db = db) {
        deleteWhere {
            (GroupMemberTable.group eq group) and
                (GroupMemberTable.member eq member)
        }
    }
}