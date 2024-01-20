package xyz.xszq.nereides.event

import io.ktor.client.statement.*
import korlibs.io.file.baseName
import korlibs.io.util.UUID
import xyz.xszq.nereides.Bot
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
                fileImage = files.first().file ?.readBytes() ?: files.first().bytes!!,
                content = if (content.all { it is LocalImage }) "" else content.text,
                replyMsgId = msgId,
                filename = files.first().file ?.baseName ?: UUID.randomUUID().toString()
            )
        } else {
            bot.sendChannelMessage(
                channelId = channelId,
                content = if (arks.isNotEmpty()) arks.first().text.replace("https://", "") else content.text,
                referMsgId = msgId,
                replyMsgId = msgId)
        }
        println(response.bodyAsText())
        if (files.size > 1) {
            files.subList(1, files.size).forEach { file ->
                response = bot.sendChannelMessageByMultipart(
                    channelId = channelId,
                    fileImage = file.file ?.readBytes() ?: file.bytes!!,
                    content = "",
                    replyMsgId = msgId,
                    filename = file.file ?.baseName ?: UUID.randomUUID().toString()
                )
                println(response.bodyAsText())
            }
        }
        return true
    }
}