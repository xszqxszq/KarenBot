package xyz.xszq.bot

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import xyz.xszq.bot.database.*
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.util.RateLimiter

/**
 * 群聊及用户信息缓存
 *
 * @property api 与 QQ 服务器通信的客户端
 * @property bot 机器人
 * @property database 数据库连接
 */
class InfoCache(
    private val api: OpenAPI,
    private val bot: Bot,
    private val database: Database,
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val logger = KotlinLogging.logger {}
    private val refreshes = RateLimiter(REFRESH_QPM, now)

    /**
     * 载入缓存
     */
    suspend fun load() {
        GroupInfoTable.forEach(database) { info ->
            val group = bot.group(info.id)
            group.name = info.name
            group.description = info.description
            group.category = info.category
            group.tags = info.tags
            group.memberCount = info.memberCount
            group.botJoinedAt = info.botJoinedAt
            group.allowPush = info.allowPush
            group.receiveMessageSetting = info.receiveMessageSetting
            group.botRole = info.botRole
            group.muteMode = info.muteMode
            group.fetchedAt = info.fetchedAt
        }
        UserInfoTable.forEach(database) { info ->
            bot.users[info.id] = User(bot, info.id, info.username, info.isBot)
        }
        GroupMemberTable.forEach(database) { group, member ->
            bot.group(group).members[member] = MemberRole.Member
        }
        logger.info {
            "[缓存] 群 ${bot.groups.size} 个 用户 ${bot.users.size} 个已载入"
        }
    }

    /**
     * 记录群消息并获取群信息缓存
     *
     * 不阻塞群信息返回
     *
     * @param group 群 OpenID
     * @param member 成员 OpenID
     * @param username 成员昵称
     * @param isBot 成员是否为 Bot
     * @param role 成员在群内的身份
     * @return 群信息
     */
    fun visit(
        group: String,
        member: String,
        username: String,
        isBot: Boolean,
        role: MemberRole
    ): Group {
        val cached = bot.group(group)
        updateUser(member, username, isBot)
        remember(cached, member, role)
        refreshIfStale(cached)
        return cached
    }

    /**
     * 存入新的群成员
     *
     * @param group 群 OpenID
     * @param member 成员 OpenID
     */
    fun join(group: String, member: String) {
        val cached = bot.group(group)
        remember(cached, member, MemberRole.Member)
    }

    /**
     * 删除已退群成员
     *
     * @param group 群 OpenID
     * @param member 成员 OpenID
     */
    fun remove(group: String, member: String) {
        bot.groups[group] ?.members ?.remove(member)
        scope.launch { removeMember(group, member) }
    }

    private fun remember(group: Group, member: String, role: MemberRole) {
        if (group.members.put(member, role) == null)
            scope.launch { persistMember(group.id, member) }
    }

    private fun updateUser(
        id: String,
        username: String,
        isBot: Boolean
    ): User {
        val known = bot.users[id]
        if (known == null) {
            val user = User(bot, id, username, isBot)
            bot.users[id] = user
            scope.launch { persistUser(user) }
            return user
        }
        if (known.username != username) {
            known.username = username
            scope.launch { persistUser(known) }
        }
        return known
    }

    private fun refreshIfStale(group: Group) {
        val current = now()
        if (current - group.fetchedAt < INFO_TTL ||
            current < group.nextRefreshAt
        ) {
            group.flushWaiting()
            return
        }
        if (!group.refreshing.compareAndSet(false, true))
            return
        scope.launch {
            runCatching {
                refresh(group)
            }.onFailure { e ->
                logger.error(e) {
                    "[缓存] 拉取群信息失败: ${group.logPrefix}"
                }
                group.nextRefreshAt = now() + FAILURE_BACKOFF
            }
            group.refreshing.set(false)
            group.flushWaiting()
        }
    }

    private suspend fun refresh(group: Group) {
        if (!refreshes.tryAcquire()) {
            group.nextRefreshAt = now() + FAILURE_BACKOFF
            return
        }
        val info = api.getGroupInfo(group.id)
        if (info == null) {
            group.nextRefreshAt = now() + FAILURE_BACKOFF
            return
        }
        val state = api.getBotState(group.id)
        group.name = info.name
        group.description = info.description
        group.category = info.category
        group.tags = info.tags
        group.memberCount = info.memberCount
        state ?.let {
            group.botJoinedAt = it.joinedAt
            group.allowPush = it.allowPush
            group.receiveMessageSetting = it.recvMsgSetting
            group.botRole = MemberRole.of(it.role)
        }
        group.fetchedAt = now()
        // 落库不阻塞群信息就绪
        scope.launch { persistGroup(group) }
    }

    private suspend fun persistGroup(group: Group) = runCatching {
        GroupInfoTable.save(database, GroupInfo(
            id = group.id,
            name = group.name,
            description = group.description,
            category = group.category,
            tags = group.tags,
            memberCount = group.memberCount,
            botJoinedAt = group.botJoinedAt,
            allowPush = group.allowPush,
            receiveMessageSetting = group.receiveMessageSetting,
            botRole = group.botRole,
            muteMode = group.muteMode,
            fetchedAt = group.fetchedAt
        ))
    }.onFailure { e ->
        logger.error(e) { "[缓存] 写入群信息失败: ${group.logPrefix}" }
    }

    private suspend fun persistMember(
        group: String,
        member: String
    ) = runCatching {
        GroupMemberTable.add(database, group, member)
    }.onFailure { e ->
        logger.error(e) {
            "[缓存] 写入群成员失败: ${bot.groupTag(group)} " +
                bot.userTag(member)
        }
    }

    private suspend fun removeMember(
        group: String,
        member: String
    ) = runCatching {
        GroupMemberTable.remove(database, group, member)
    }.onFailure { e ->
        logger.error(e) {
            "[缓存] 移除群成员失败: ${bot.groupTag(group)} " +
                bot.userTag(member)
        }
    }

    private suspend fun persistUser(user: User) = runCatching {
        UserInfoTable.save(
            database,
            UserInfo(user.id, user.username, user.isBot)
        )
    }.onFailure { e ->
        logger.error(e) { "[缓存] 写入用户信息失败: ${user.logPrefix}" }
    }

    private companion object {
        const val INFO_TTL = 24 * 60 * 60 * 1000L
        const val FAILURE_BACKOFF = 10 * 60 * 1000L
        const val REFRESH_QPM = 30
    }
}