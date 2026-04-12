package xyz.xszq.bot.subscribe

import xyz.xszq.bot.event.MessageEvent

/**
 * Subscribe of message ends with suffix.
 * @param parent Parent prefix of command.
 * @param forceParent Force check of parent prefix.
 * @param suffix The command's suffix.
 * @param handler Handle after matched.
 */
class CommandEndsWith(
    parent: String? = null,
    forceParent: Boolean = false,
    private val suffix: String,
    private val matchHandler: suspend MessageEvent.(String) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 2
    override val length = suffix.length

    override fun matchesText(message: String): Boolean {
        val args = message.split(" ")
        if (args.isEmpty())
            return false
        return args.first().endsWith(suffix)
    }

    override suspend fun handleText(event: MessageEvent, message: String) {
        val args = message.split(" ")
        val command = args.first()
        val nowArgs = command.substringBefore(suffix).trim() + " " +
                args.subList(1, args.size).joinToString(" ")
        matchHandler(event, nowArgs)
    }
}