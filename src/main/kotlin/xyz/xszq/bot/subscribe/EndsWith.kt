package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent


/**
 * Subscribe of message ends with suffix.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param suffix The command's suffix.
 * @param handler Handle after matched.
 */
class EndsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    private val suffix: String,
    private val matchHandler: suspend MessageEvent.(String) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 1
    override val length = suffix.length

    override fun matchesText(message: String) = message.endsWith(suffix)

    override suspend fun handleText(event: MessageEvent, message: String) {
        val arg = message.substringBefore(suffix).trim()
        matchHandler(event, arg)
    }
}