package xyz.xszq.nereides.event

import xyz.xszq.nereides.Bot

class BotLeaveGroupEvent(
    override val bot: Bot,
    override val groupId: String,
    val operator: String
) : GroupEvent