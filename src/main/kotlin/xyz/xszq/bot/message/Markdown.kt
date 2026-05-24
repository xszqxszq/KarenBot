package xyz.xszq.bot.message

import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.MarkdownDsl

class Markdown(
    val markdown: MarkdownData,
    val keyboard: Keyboard ?= null
): MessageElement {
    override val content: String = "[Markdown] ${markdown.text()}\n${keyboard?.text()}"

    companion object {
        fun create(block: MarkdownDsl.() -> Unit) =
            MarkdownDsl().apply(block).build()
    }
}