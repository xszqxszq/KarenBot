package xyz.xszq.nereides.event

import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.payload.user.BotUser

class BotReadyEvent(override val client: QQClient, val user: BotUser, val shard: List<Int>) : BotEvent {
}