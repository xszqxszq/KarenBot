package xyz.xszq.bot.message

import xyz.xszq.bot.User

/**
 * `@`用户消息
 *
 * @param user 被@的用户
 * @param isAll 是否为@全体成员
 */
@Suppress("unused")
class At(
    val user: User? = null,
    val isAll: Boolean = false
) : MessageElement {
    override val content: String get() = "[at:${user ?.id ?: "all"}]"
    override fun toString(): String = content
}