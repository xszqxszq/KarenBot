package xyz.xszq.bot

/**
 * QQ 用户
 *
 * @property username 用户昵称
 * @property isBot 是否为 Bot 而非普通用户
 * @property isSelf 是否为 Bot 自身
 */
open class User(
    val bot: Bot,
    val id: String,
    var username: String = "",
    val isBot: Boolean = false,
    val isSelf: Boolean = false,
) {
    val avatar: String get() {
        return "https://q.qlogo.cn/qqapp/${bot.api.config.appId}/$id/640"
    }

    /**
     * 日志前缀
     */
    val logPrefix: String get() = "${bot.users[id] ?.username ?: username}($id)"
}