package xyz.xszq.bot.message

import xyz.xszq.bot.User

class At(
    val user: User
) : MessageElement {
    override val content: String get() = "[at:${user.id}]"
    override fun toString(): String = content
}
