@file:Suppress("unused")

package xyz.xszq.nereides.event

class MessageSubscribeBuilder(private val prefix: String = "") {
    fun always(block: suspend MessageEvent.() -> Unit) {
        GlobalEventChannel.subscribeMessage {
            if (prefix.isBlank() || content.removeChannelAtPrefix(this).trim().startsWith(prefix))
                block(this)
        }
    }
    fun equalsTo(text: String, block: suspend MessageEvent.() -> Unit) {
        val realCommand =
            if (prefix.isNotBlank()) prefix.trim() + " " + text
            else text
        GlobalEventChannel.subscribeMessage {
            if (content.removeChannelAtPrefix(this).trim() == realCommand.trim()) {
                block(this)
            }
        }
    }
    fun startsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        val realCommand =
            if (prefix.isNotBlank()) prefix.trim() + " " + text
            else text
        GlobalEventChannel.subscribeMessage {
            if (content.removeChannelAtPrefix(this).trim().startsWith(realCommand)) {
                val arg = content.removeChannelAtPrefix(this).trim().substringAfter(realCommand).trim()
                block(this, arg)
            }
        }
    }
    fun endsWith(text: String, block: suspend MessageEvent.(String) -> Unit) {
        GlobalEventChannel.subscribeMessage {
            if (content.removeChannelAtPrefix(this).trim().endsWith(text)) {
                val arg = content.removeChannelAtPrefix(this).substringAfter(prefix).trim().substringBefore(text).trim()
                block(this, arg)
            }
        }
    }
    private fun String.removeChannelAtPrefix(event: MessageEvent): String {
        return if (event is GuildAtMessageEvent)
            substringAfter("<@!${event.client.botGuildInfo.id}>")
        else
            this
    }
}