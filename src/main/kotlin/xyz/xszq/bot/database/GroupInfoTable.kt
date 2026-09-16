package xyz.xszq.bot.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import xyz.xszq.bot.MemberRole

/**
 * 群聊信息缓存表
 */
object GroupInfoTable: Table() {
    /**
     * 群 OpenID
     */
    val id = varchar("id", 32)
    /**
     * 群名称
     */
    val name = varchar("name", 128)
    /**
     * 群简介
     */
    val description = text("description")
    /**
     * 群分类
     */
    val category = varchar("category", 64)
    /**
     * 群标签列表
     */
    val tags = text("tags")
    /**
     * 群成员人数
     */
    val memberCount = integer("member_count")
    /**
     * 机器人入群时间
     */
    val botJoinedAt = varchar("bot_joined_at", 64)
    /**
     * 是否接收主动推送
     */
    val allowPush = bool("allow_push")
    /**
     * 机器人接收消息的类型
     */
    val receiveMessageSetting = varchar("receive_message_setting", 32)
    /**
     * 机器人在群内的身份
     */
    val botRole = varchar("bot_role", 16)
    /**
     * 禁言模式
     */
    val muteMode = varchar("mute_mode", 16)
    /**
     * 上次成功拉取的时间（毫秒）
     */
    val fetchedAt = long("fetched_at")
    override val primaryKey = PrimaryKey(id)

    private fun encodeTags(tags: List<String>) = tags.joinToString("\n")

    private fun decodeTags(raw: String) = when {
        raw.isEmpty() -> listOf()
        else -> raw.split("\n")
    }

    /**
     * 遍历全部群信息
     *
     * @param db 数据库连接
     * @param block 处理逻辑
     */
    suspend fun forEach(
        db: Database,
        block: (GroupInfo) -> Unit
    ) = newSuspendedTransaction(db = db) {
        selectAll().forEach { row ->
            block(GroupInfo(
                id = row[GroupInfoTable.id],
                name = row[name],
                description = row[description],
                category = row[category],
                tags = decodeTags(row[tags]),
                memberCount = row[memberCount],
                botJoinedAt = row[botJoinedAt],
                allowPush = row[allowPush],
                receiveMessageSetting = row[receiveMessageSetting],
                botRole = MemberRole.of(row[botRole]),
                muteMode = row[muteMode],
                fetchedAt = row[fetchedAt]
            ))
        }
    }

    /**
     * 保存群信息缓存
     *
     * @param db 数据库连接
     * @param info 群信息
     */
    suspend fun save(
        db: Database,
        info: GroupInfo
    ) = newSuspendedTransaction(db = db) {
        val match = GroupInfoTable.id eq info.id
        if (selectAll().where { match }.count() != 0L)
            update({ match }) { it.assign(info) }
        else
            insert {
                it[id] = info.id
                it.assign(info)
            }
    }

    /**
     * 写入群信息字段
     *
     * @param info 群信息
     */
    private fun UpdateBuilder<*>.assign(info: GroupInfo) {
        this[name] = info.name
        this[description] = info.description
        this[category] = info.category
        this[tags] = encodeTags(info.tags)
        this[memberCount] = info.memberCount
        this[botJoinedAt] = info.botJoinedAt
        this[allowPush] = info.allowPush
        this[receiveMessageSetting] = info.receiveMessageSetting
        this[botRole] = info.botRole.name.lowercase()
        this[muteMode] = info.muteMode
        this[fetchedAt] = info.fetchedAt
    }
}