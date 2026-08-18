package xyz.xszq.bot.message

import xyz.xszq.bot.payload.ArkData

class Ark(
    val data: ArkData
): MessageElement {
    override val content: String = "[卡片消息] ${data.arkName}\n摘要: ${data.prompt}"

    init {
        data.parsedFields()
    }
}