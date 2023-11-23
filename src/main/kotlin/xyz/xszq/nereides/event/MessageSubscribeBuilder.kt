@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq.nereides.event

class MessageSubscribeBuilder(private val prefix: String = "", val force: Boolean = false) { // TODO: Force and Not force
    fun always(block: suspend MessageEvent.() -> Unit) {
        GlobalEventChannel.subscribeMessage {
            if (prefix.isBlank() || !force || content.removeChannelAtPrefix(this).trim().startsWith(prefix))
                block(this)
        }
    }
    fun equalsTo(text: List<String>, block: suspend MessageEvent.() -> Unit) {
        val list = if (prefix.isNotBlank()) {
            val nowList = text.map {
                prefix.trim() + " " + it
            }.toMutableList()
            if (!force)
                nowList.addAll(text)
            nowList
        } else text
        GlobalEventChannel.subscribeMessage {
            if (content.removeChannelAtPrefix(this).trim() in list) {
                block(this)
            }
        }
    }
    fun equalsTo(text: String, block: suspend MessageEvent.() -> Unit) = equalsTo(listOf(text), block)
    fun startsWith(text: List<String>, block: suspend MessageEvent.(String) -> Unit) {
        val list = if (prefix.isNotBlank()) {
            val nowList = text.map {
                prefix.trim() + " " + it
            }.toMutableList()
            if (!force)
                nowList.addAll(text)
            nowList
        } else text
        GlobalEventChannel.subscribeMessage {
            list.find {
                content.removeChannelAtPrefix(this).trim().startsWith(it)
            } ?.let {
                val arg = content.removeChannelAtPrefix(this).trim().substringAfter(it).trim()
                block(this, arg)
            }
        }
    }
    fun startsWith(text: String, block: suspend MessageEvent.(String) -> Unit) = startsWith(listOf(text), block)
    fun endsWith(text: List<String>, block: suspend MessageEvent.(String) -> Unit) {
        val list = if (prefix.isNotBlank()) {
            val nowList = text.map {
                prefix.trim() + " " + it
            }.toMutableList()
            if (!force)
                nowList.addAll(text)
            nowList
        } else text
        GlobalEventChannel.subscribeMessage {
            list.find {
                content.removeChannelAtPrefix(this).trim().endsWith(it)
            } ?.let {
                val arg = content.removeChannelAtPrefix(this).substringAfter(prefix).trim().substringBefore(it).trim()
                block(this, arg)
            }
        }
    }
    fun endsWith(text: String, block: suspend MessageEvent.(String) -> Unit) = endsWith(listOf(text), block)
    private fun String.removeChannelAtPrefix(event: MessageEvent): String {
        return if (event is GuildAtMessageEvent)
            substringAfter("<@!${event.client.botGuildInfo.id}>")
        else
            this
    }
}