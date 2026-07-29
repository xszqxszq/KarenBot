package xyz.xszq.bot.message

import xyz.xszq.bot.User

@Suppress("unused")
class At(
    val user: User? = null,
    val isAll: Boolean = false
) : MessageElement {
    override val content: String get() = "[at:${user ?.id ?: "all"}]"
    override fun toString(): String = content
}
