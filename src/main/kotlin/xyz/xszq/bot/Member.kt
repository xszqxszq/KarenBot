package xyz.xszq.bot

@Suppress("unused")
class Member(
    bot: Bot,
    id: String,
    username: String = "",
    isBot: Boolean = false,
    isSelf: Boolean = false,
    val role: MemberRole = MemberRole.Member
): User(
    bot = bot,
    id = id,
    username = username,
    isBot = isBot,
    isSelf = isSelf,
)