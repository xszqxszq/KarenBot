package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * Subscribe of message equals to string.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param text The command.
 * @param handler Handle after matched.
 */
class EqualsTo(
    parent: String? = null,
    forceParent: Boolean = false,
    private val text: String,
    private val matchHandler: suspend MessageEvent.() -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 4
    override val length = text.length

    override fun matchesText(message: String) = message == text

    override suspend fun handleText(event: MessageEvent, message: String) {
        matchHandler(event)
    }
}