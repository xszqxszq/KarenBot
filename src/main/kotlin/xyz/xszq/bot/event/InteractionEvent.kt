package xyz.xszq.bot.event

import xyz.xszq.bot.Bot
import xyz.xszq.bot.User

/**
 * 互动事件
 *
 * @param button 按钮 ID
 * @param data 按钮数据
 */
open class InteractionEvent(
    override val bot: Bot,
    override val eventId: String,
    override var id: String,
    var data: String,
    val button: String,
    open val sender: User,
    override var seq: Int = 1
): UserReplyAbleEvent {
    override val user: User = sender
    val content = "[互动,$button,$data]"
}