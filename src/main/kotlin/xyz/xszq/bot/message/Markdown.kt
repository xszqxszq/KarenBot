package xyz.xszq.bot.message

import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData

class Markdown(
    val markdown: MarkdownData,
    val keyboard: Keyboard ?= null
): MessageElement {
    override val content: String = "[Markdown] ${markdown.text()}\n${keyboard?.text()}"
}