package xyz.xszq.bot.event

import korlibs.io.util.UUID
import xyz.xszq.bot.Bot

/**
 * 频道事件
 *
 * 用于插件之间互相通信
 *
 * @param channelName 频道名
 * @param data 数据
 */
class ChannelEvent<T: Any>(
    override val bot: Bot,
    override val eventId: String = UUID.randomUUID().toString(),
    val channelName: String,
    val data: T
): Event