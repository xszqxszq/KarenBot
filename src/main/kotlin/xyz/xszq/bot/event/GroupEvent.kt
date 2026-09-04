package xyz.xszq.bot.event

import xyz.xszq.bot.Group

/**
 * 群事件
 *
 * @property group 群组
 */
interface GroupEvent: Event {
    val group: Group
}