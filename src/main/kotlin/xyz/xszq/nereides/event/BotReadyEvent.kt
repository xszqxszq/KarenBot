package xyz.xszq.nereides.event

import xyz.xszq.nereides.Bot
import xyz.xszq.nereides.payload.user.BotUser

class BotReadyEvent(override val bot: Bot, val user: BotUser, val shard: List<Int>) : BotEvent {
}