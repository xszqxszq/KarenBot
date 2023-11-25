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
            if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                return@subscribePublicMessage
            if (prefix.isBlank() || !forcePrefix || content.removeChannelAtPrefix(this).trim().startsWith(prefix))
                block(this)
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
            if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                return@subscribePublicMessage
            if (content.removeChannelAtPrefix(this).trim() in list) {
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
            if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                return@subscribePublicMessage
            list.find {
                content.removeChannelAtPrefix(this).trim().startsWith(it)
            } ?.let {
                val arg = content.removeChannelAtPrefix(this).trim().substringAfter(it).trim()
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
            if (permName.isNotBlank() && Permissions.isNotPermitted(contextId, permName))
                return@subscribePublicMessage
            list.find {
                content.removeChannelAtPrefix(this).trim().endsWith(it)
            } ?.let {
                val arg = content.removeChannelAtPrefix(this).substringAfter(prefix).trim().substringBefore(it).trim()
                block(this, arg)
            }
        }
    }
    fun endsWith(text: String, block: suspend PublicMessageEvent.(String) -> Unit) = endsWith(listOf(text), block)
    private fun String.removeChannelAtPrefix(event: PublicMessageEvent): String {
        return if (event is GuildAtMessageEvent)
            substringAfter("<@!${event.client.botGuildInfo.id}>")
        else
            this
    }
}