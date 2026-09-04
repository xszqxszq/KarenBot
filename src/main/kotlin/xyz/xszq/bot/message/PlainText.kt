package xyz.xszq.bot.message

/**
 * 纯文本消息
 * @param content 消息文本
 */
class PlainText(
    override val content: String
) : MessageElement {
    override fun toString(): String = content
}