package xyz.xszq.bot.event

import xyz.xszq.bot.User

interface UserEvent: Event {
    val user: User
}