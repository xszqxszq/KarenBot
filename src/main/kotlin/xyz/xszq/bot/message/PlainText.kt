package xyz.xszq.bot.message

/**
 * Plain Text Message.
 * @param content The text of message.
 */
class PlainText(
    override val content: String
) : MessageElement {
    override fun toString(): String = content
}