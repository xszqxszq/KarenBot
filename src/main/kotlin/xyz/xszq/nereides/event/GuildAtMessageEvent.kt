package xyz.xszq.nereides.event

import com.soywiz.korio.file.VfsFile
import xyz.xszq.nereides.QQClient
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.user.GuildUser
import java.io.File

class GuildAtMessageEvent(
    override val client: QQClient,
    override val msgId: String,
    override val channelId: String,
    override val guildId: String,
    override val subjectId: String,
    override val content: String,
    override val timestamp: Long,
    val author: GuildUser,
    val mentions: List<GuildUser>? = null
) : GuildChannelEvent, PublicMessageEvent(channelId) {
    override suspend fun reply(content: String) {
        client.sendChannelMessage(channelId, content, referMsgId = msgId, replyMsgId = msgId)
    }
    override suspend fun sendImage(url: String) {
    }

    override suspend fun sendImage(file: File) {
    }

    override suspend fun sendImage(file: VfsFile) {
    }

    override suspend fun sendImage(binary: ByteArray) {
    }

    override suspend fun sendArk(messageArk: MessageArk) {
    }

}