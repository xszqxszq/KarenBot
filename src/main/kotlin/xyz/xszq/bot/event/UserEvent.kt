package xyz.xszq.bot.event

import xyz.xszq.bot.User

/**
 * 用户相关事件
 *
 * @property user 相关用户
 */
interface UserEvent: Event {
    val user: User
}