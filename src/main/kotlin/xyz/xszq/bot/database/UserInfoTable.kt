package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * 用户信息缓存表
 */
object UserInfoTable: Table() {
    /**
     * 用户 OpenID
     */
    val id = varchar("id", 32)
    /**
     * 用户昵称
     */
    val username = varchar("username", 128)
    /**
     * 是否为机器人
     */
    val isBot = bool("is_bot")
    override val primaryKey = PrimaryKey(id)

    /**
     * 遍历全部用户信息
     *
     * @param db 数据库连接
     * @param block 处理逻辑
     */
    suspend fun forEach(
        db: Database,
        block: (UserInfo) -> Unit
    ) = newSuspendedTransaction(db = db) {
        selectAll().forEach { row ->
            block(UserInfo(
                id = row[UserInfoTable.id],
                username = row[UserInfoTable.username],
                isBot = row[UserInfoTable.isBot]
            ))
        }
    }

    /**
     * 存入用户信息缓存
     *
     * @param db 数据库连接
     * @param info 用户信息
     */
    suspend fun save(
        db: Database,
        info: UserInfo
    ) = newSuspendedTransaction(db = db) {
        val match = UserInfoTable.id eq info.id
        if (selectAll().where { match }.count() != 0L)
            update({ match }) {
                it[username] = info.username
                it[isBot] = info.isBot
            }
        else
            insert {
                it[id] = info.id
                it[username] = info.username
                it[isBot] = info.isBot
            }
    }
}