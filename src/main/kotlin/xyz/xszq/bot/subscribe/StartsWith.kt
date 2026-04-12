package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * Subscribe of message starts with prefix.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param prefix The command's prefix.
 * @param handler Handle after matched.
 */
class StartsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    private val prefix: String,
    private val matchHandler: suspend MessageEvent.(String) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 3
    override val length = prefix.length

    override fun matchesText(message: String) = message.startsWith(prefix)

    override suspend fun handleText(event: MessageEvent, message: String) {
        val arg = message.substringAfter(prefix).trim()
        matchHandler(event, arg)
    }
}