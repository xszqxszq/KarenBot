package xyz.xszq.nereides

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.statement.*
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.MessageEmbed
import xyz.xszq.nereides.payload.message.MessageMarkdown
import xyz.xszq.nereides.payload.message.MessageReference
import xyz.xszq.nereides.payload.post.PostChannelMessage
import xyz.xszq.nereides.payload.user.GuildUser

interface GuildApi {
    val logger: KLogger
    suspend fun get(api: String): HttpResponse
    suspend fun post(api: String, payload: Any)
    suspend fun sendChannelMessage(
        channelId: String,
        content: String? = null,
        image: String? = null,
        embed: MessageEmbed? = null,
        ark: MessageArk? = null,
        markdown: MessageMarkdown? = null,
        eventId: String? = null,
        replyMsgId: String? = null,
        referMsgId: String? = null
    ) = post("/channels/$channelId/messages", PostChannelMessage(
        content = content,
        image = image,
        embed = embed,
        ark = ark,
        markdown = markdown,
        eventId = eventId,
        msgId = replyMsgId,
        messageReference = referMsgId ?.let {
            MessageReference(referMsgId, true)
        }
    ))
    suspend fun sendChannelText(
        channelId: String,
        content: String,
        replyMsgId: String? = null,
        referMsgId: String? = null,
        eventId: String? = null
    ) = sendChannelMessage(
        channelId = channelId,
        content = content,
        replyMsgId = replyMsgId,
        referMsgId = referMsgId
    )
    suspend fun sendChannelImageByUrl(
        channelId: String,
        url: String,
        replyMsgId: String? = null,
        referMsgId: String? = null,
        eventId: String? = null
    ) = sendChannelMessage(
        channelId = channelId,
        image = url,
        replyMsgId = replyMsgId,
        referMsgId = referMsgId
    )
    suspend fun getBotInfo() = get("/users/@me").body<GuildUser>()
}