package xyz.xszq.nereides

import xyz.xszq.nereides.message.MessageChain

interface Context {
    val id: String
    val bot: Bot
    suspend fun sendMessage(content: MessageChain): Boolean
}