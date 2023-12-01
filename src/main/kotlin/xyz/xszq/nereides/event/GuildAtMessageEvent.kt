package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.message.*
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import xyz.xszq.nereides.payload.user.GuildUser
import xyz.xszq.nereides.payload.utils.FileType
import xyz.xszq.nereides.payload.utils.MsgType
import java.io.File

class GuildAtMessageEvent(
    override val client: QQClient,
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
        val response = client.sendChannelMessage(channelId, content, referMsgId = msgId, replyMsgId = msgId)
        return true
    }

    override suspend fun reply(content: Message) = reply(MessageChain(content))

    override suspend fun reply(content: MessageChain): Boolean {
        val files = content.filterIsInstance<LocalImage>()
        var response = if (files.isNotEmpty()) {
            client.sendChannelMessageByMultipart(
                channelId = channelId,
                fileImage = files.first().file,
                content = content.text.ifBlank { "" },
                replyMsgId = msgId
            )
        } else {
            client.sendChannelMessage(
                channelId = channelId,
                content = content.text,
                referMsgId = msgId,
                replyMsgId = msgId)
        }
        println(response.bodyAsText())
        if (files.size > 1) {
            files.subList(1, files.size).forEach { file ->
                response = client.sendChannelMessageByMultipart(
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