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
    private val matchHandler: suspend MessageEvent.(Pair<String, String?>) -> Unit
): TextSubscribe(parent, forceParent) {
    override val priority = 2
    override val length = suffix.length

    override fun matchesText(message: String): Boolean {
        val args = message.split(" ")
        if (args.isEmpty())
            return false
        return args.last().endsWith(suffix)
                || (args.size >= 2 && args[args.size - 2].endsWith(suffix))
    }

    override suspend fun handleText(event: MessageEvent, message: String) {
        val args = message.split(" ")
        val matchingIndex = when {
            args.last().endsWith(suffix) -> args.size - 1
            args.size >= 2 && args[args.size - 2].endsWith(suffix) -> args.size - 2
            else -> return
        }
        val matchingToken = args[matchingIndex]

        val command = when (matchingIndex) {
            0 -> matchingToken.removeSuffix(suffix).takeIf { it.isNotEmpty() } ?: matchingToken
            else -> {
                val prefixPart = args.subList(0, matchingIndex).joinToString(" ")
                val suffixPart = matchingToken.removeSuffix(suffix)
                if (suffixPart.isNotEmpty()) "$prefixPart $suffixPart" else prefixPart
            }
        }

        val remaining = args.drop(matchingIndex + 1)
            .joinToString(" ")
            .trim()
            .takeIf { it.isNotEmpty() }

        matchHandler(event, command to remaining)
    }
}