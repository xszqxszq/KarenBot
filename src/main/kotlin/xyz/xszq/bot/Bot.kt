package xyz.xszq.bot

import xyz.xszq.bot.payload.UsersMeResponse
import xyz.xszq.bot.service.OpenAPI
import xyz.xszq.bot.service.TencentCOS
import java.util.concurrent.ConcurrentHashMap

/**
 * Bot 类
 *
 * @property api 与 QQ 服务器通信的客户端
 * @property cos 腾讯云 COS 客户端
 * @property me 机器人自身详情
 * @property groups 加入的群聊
 * @property users 用户信息
 */
class Bot(
    val api: OpenAPI,
    val cos: TencentCOS,
    val me: UsersMeResponse ?= null
) {
    val self: User by lazy {
        User(this, me ?.id ?: "", me ?.username ?: "", isBot = true, isSelf = true)
    }
    val groups: ConcurrentHashMap<String, Group> = ConcurrentHashMap()
    val users: ConcurrentHashMap<String, User> = ConcurrentHashMap()

    /**
     * 获取群信息
     *
     * @param id 群 OpenID
     */
    fun group(id: String): Group =
        groups.computeIfAbsent(id) { Group(this, it) }

    /**
     * 获取用户在日志中的标识
     *
     * @param id 用户 OpenID
     */
    fun userTag(id: String): String = users[id] ?.logPrefix ?: "($id)"

    /**
     * 获取群在日志中的标识
     *
     * @param id 群 OpenID
     */
    fun groupTag(id: String): String = groups[id] ?.logPrefix ?: "[$id]"
}