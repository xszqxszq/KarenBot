package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.message.Message
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.response.PostGroupMessageResponse
import xyz.xszq.nereides.payload.user.GuildUser
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
    override suspend fun reply(content: String): PostGroupMessageResponse? {
        client.sendChannelMessage(channelId, content, referMsgId = msgId, replyMsgId = msgId)
        return null
    }

    override suspend fun reply(content: Message) = reply(MessageChain(content))

    override suspend fun reply(content: MessageChain): PostGroupMessageResponse? {
        TODO()
    }

}