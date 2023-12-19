package xyz.xszq.nereides.event

import io.ktor.client.statement.*
import kotlinx.coroutines.*
import xyz.xszq.nereides.Bot
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.message.Ark
import xyz.xszq.nereides.message.LocalImage
import xyz.xszq.nereides.message.Message
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.message.ark.ListArk
import xyz.xszq.nereides.payload.user.GuildUser

class GuildAtMessageEvent(
    override val bot: Bot,
    override val msgId: String,
    override val channelId: String,
    override val guildId: String,
    override val subjectId: String,
    override val message: MessageChain,
    override val contentString: String = message.contentToString(),
    override val timestamp: Long,
    val author: GuildUser,
    val mentions: List<GuildUser>? = null
) : GuildChannelEvent, PublicMessageEvent(channelId) {
    override suspend fun reply(content: String): Boolean { // TODO: Success check
        bot.sendChannelMessage(channelId, content, referMsgId = msgId, replyMsgId = msgId)
        return true
    }

    override suspend fun reply(content: Message) = reply(MessageChain(content))

    override suspend fun reply(content: MessageChain): Boolean {
        val files = content.filterIsInstance<LocalImage>()
        val arks = content.filterIsInstance<ListArk>()
        var response = if (files.isNotEmpty()) {
            bot.sendChannelMessageByMultipart(
                channelId = channelId,
                fileImage = files.first().file,
                content = content.text.ifBlank { "" },
                replyMsgId = msgId
            )
        } else {
            bot.sendChannelMessage(
                channelId = channelId,
                content = if (arks.isNotEmpty()) arks.first().text else content.text,
                referMsgId = msgId,
                replyMsgId = msgId)
        }
        println(response.bodyAsText())
        if (files.size > 1) {
            files.subList(1, files.size).forEach { file ->
                response = bot.sendChannelMessageByMultipart(
                    channelId = channelId,
                    fileImage = file.file,
                    content = "",
                    replyMsgId = msgId
                )
                println(response.bodyAsText())
            }
        }
        return true
    }
}