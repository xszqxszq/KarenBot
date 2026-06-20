package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User
import xyz.xszq.bot.message.MessageChain

open class MessageEvent(
    override val bot: Bot,
    override val eventId: String,
    override val id: String,
    val message: MessageChain,
    open val sender: User,
    override var seq: Int = 1,
    open val reference: MessageChain ?= null
): UserReplyAbleEvent {
    override val user: User = sender
    val content get() = message.content
    val text get() = message.text
}