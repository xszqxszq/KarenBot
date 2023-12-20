package xyz.xszq.nereides.message

import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.payload.message.MessageMarkdownC2C

class Markdown(val markdown: MessageMarkdownC2C): Message {
    override fun contentToString(): String {
        return "[markdown]"
    }
    companion object {
        suspend fun build(templateId: String, block: suspend MessageMarkdownC2C.Builder.() -> Unit): Markdown {
            val builder = MessageMarkdownC2C.Builder()
            block(builder)
            return Markdown(builder.build(templateId))
        }
    }
}