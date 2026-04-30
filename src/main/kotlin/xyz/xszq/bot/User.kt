package xyz.xszq.bot

class User(
    val bot: Bot,
    val id: String,
    val username: String = "",
) {
    val avatar: String get() {
        return "https://q.qlogo.cn/qqapp/${bot.api.config.appId}/$id/640"
    }
}