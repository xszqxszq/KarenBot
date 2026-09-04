package xyz.xszq.bot.message

import xyz.xszq.bot.payload.markdown.Keyboard
import xyz.xszq.bot.payload.markdown.MarkdownData
import xyz.xszq.bot.payload.markdown.MarkdownDsl

/**
 * Markdown 消息
 *
 * @param markdown Markdown 内容数据
 * @param keyboard 下方的按钮
 */
class Markdown(
    val markdown: MarkdownData,
    val keyboard: Keyboard ?= null
): MessageElement {
    override val content: String = "[Markdown] ${markdown.text()}\n${keyboard?.text()}"

    companion object {
        /**
         * 通过 DSL 构建 Markdown 消息
         *
         * @param block 构建的 DSL
         * @return Markdown 消息
         */
        fun create(block: MarkdownDsl.() -> Unit) =
            MarkdownDsl().apply(block).build()
    }
}