@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xszq.nereides.event

import xyz.xszq.bot.dao.Permissions

class PublicMessageSubscribeBuilder(
    private val prefix: String = "",
    val forcePrefix: Boolean = false,
    val permName: String = ""
) {
    fun always(block: suspend PublicMessageEvent.() -> Unit) {
        GlobalEventChannel.subscribePublicMessage {
            if (prefix.isBlank() || !forcePrefix || message.text.removeChannelAtPrefix(this).trim().startsWith(prefix)) {
                if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                    return@subscribePublicMessage
                block(this)
            }
        }
    }
    fun equalsTo(text: List<String>, block: suspend PublicMessageEvent.() -> Unit) {
        val list = if (prefix.isNotBlank()) {
            val nowList = text.map {
                prefix.trim() + " " + it
            }.toMutableList()
            if (!forcePrefix)
                nowList.addAll(text)
            nowList
        } else text
        GlobalEventChannel.subscribePublicMessage {
            if (message.text.removeChannelAtPrefix(this).trim() in list) {
                if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                    return@subscribePublicMessage
                block(this)
            }
        }
    }
    fun equalsTo(text: String, block: suspend PublicMessageEvent.() -> Unit) = equalsTo(listOf(text), block)
    fun startsWith(text: List<String>, block: suspend PublicMessageEvent.(String) -> Unit) {
        val list = if (prefix.isNotBlank()) {
            val nowList = text.map {
                prefix.trim() + " " + it
            }.toMutableList()
            if (!forcePrefix)
                nowList.addAll(text)
            nowList
        } else text

        GlobalEventChannel.subscribePublicMessage {
            list.find {
                message.text.trim().startsWith(it)
            } ?.let {
                if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                    return@subscribePublicMessage
                val arg = message.text.trim().substringAfter(it).trim()
                block(this, arg)
            }
        }
    }
    fun startsWith(text: String, block: suspend PublicMessageEvent.(String) -> Unit) = startsWith(listOf(text), block)
    fun endsWith(text: List<String>, block: suspend PublicMessageEvent.(String) -> Unit) {
        val list = if (prefix.isNotBlank()) {
            val nowList = text.map {
                prefix.trim() + " " + it
            }.toMutableList()
            if (!forcePrefix)
                nowList.addAll(text)
            nowList
        } else text
        GlobalEventChannel.subscribePublicMessage {
            list.find {
                message.text.removeChannelAtPrefix(this).trim().endsWith(it)
            } ?.let {
                if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                    return@subscribePublicMessage
                val arg = message.text.removeChannelAtPrefix(this).substringAfter(prefix).trim().substringBefore(it).trim()
                block(this, arg)
            }
        }
    }
    fun endsWith(text: String, block: suspend PublicMessageEvent.(String) -> Unit) = endsWith(listOf(text), block)
    private fun String.removeChannelAtPrefix(event: PublicMessageEvent): String {
        return if (event is GuildAtMessageEvent)
            substringAfter("<@!${event.bot.botGuildInfo.id}>")
        else
            this
    }
}