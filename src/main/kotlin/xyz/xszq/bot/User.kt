package xyz.xszq.bot

class User(
    val bot: Bot,
    val id: String,
    val username: String = "",
    val isBot: Boolean = false,
    val isSelf: Boolean = false,
) {
    val avatar: String get() {
        return "https://q.qlogo.cn/qqapp/${bot.api.config.appId}/$id/640"
    }
}