package xyz.xszq.bot

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * QQ 群
 *
 * @property members 群成员
 * @property fetchedAt 群信息上次拉取时间（毫秒）
 * @property nextRefreshAt 下次拉取群信息的时间（毫秒）
 * @property refreshing 是否正在拉取群信息
 */
class Group(
    val bot: Bot,
    val id: String
) {
    var name: String = ""
    var description: String = ""
    var category: String = ""
    var tags: List<String> = listOf()
    var memberCount: Int = 0
    var botJoinedAt: String = ""
    var allowPush: Boolean = false
    var receiveMessageSetting: String = ""
    var botRole: MemberRole = MemberRole.Member
    var muteMode: String = "none"
    var fetchedAt: Long = 0L
    var nextRefreshAt: Long = 0L
    val refreshing: AtomicBoolean = AtomicBoolean(false)
    val members: ConcurrentHashMap<String, MemberRole> = ConcurrentHashMap()
    private val waiting = mutableListOf<(Group) -> Unit>()

    /**
     * 群信息拉取完成
     */
    private val resolved: Boolean
        get() = fetchedAt > 0L || nextRefreshAt > 0L

    /**
     * 日志前缀
     */
    val logPrefix: String get() = "${bot.groups[id] ?.name ?: name}[$id]"

    /**
     * 群信息就绪后执行
     *
     * @param block 代码块
     */
    fun whenInfoReady(block: (Group) -> Unit) {
        val ready = synchronized(waiting) {
            if (resolved || !refreshing.get()) {
                true
            } else {
                waiting += block
                false
            }
        }
        if (ready)
            block(this)
    }

    /**
     * 执行等待群信息
     */
    internal fun flushWaiting() {
        val pending = synchronized(waiting) {
            waiting.toList().also { waiting.clear() }
        }
        pending.forEach { it(this) }
    }

    /**
     * 获取群成员列表
     */
    fun memberList(): List<Member> = members.map { (member, role) ->
        val user = bot.users[member]
        Member(
            bot = bot,
            id = member,
            username = user ?.username ?: "",
            isBot = user ?.isBot ?: false,
            role = role
        )
    }
}