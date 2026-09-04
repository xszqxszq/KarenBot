package xyz.xszq.bot

/**
 * 群成员
 */
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