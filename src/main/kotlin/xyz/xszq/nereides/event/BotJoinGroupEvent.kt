package xyz.xszq.nereides.event

import xyz.xszq.nereides.Bot

class BotJoinGroupEvent(
    override val bot: Bot,
    override val groupId: String,
    val operator: String
) : GroupEvent