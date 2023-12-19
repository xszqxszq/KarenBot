package xyz.xszq.nereides.message

import xyz.xszq.nereides.payload.message.MessageMarkdownC2C

class Markdown(val markdown: MessageMarkdownC2C): Message {
    override fun contentToString(): String {
        return "[markdown]"
    }
}