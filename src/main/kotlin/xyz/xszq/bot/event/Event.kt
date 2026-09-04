package xyz.xszq.bot.event

import xyz.xszq.bot.Bot

/**
 * 事件
 *
 * @property bot 收到事件的 Bot
 * @property eventId 事件 ID
 */
interface Event {
    val bot: Bot
    val eventId: String
}