package xyz.xszq.bot.payload

/**
 * Supported types of message send to server.
 */
@Suppress("unused")
object MsgType {
    const val TEXT = 0
    const val MARKDOWN = 2
    const val ARK = 3
    const val EMBED = 4
    const val MEDIA = 7
}