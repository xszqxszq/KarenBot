package xyz.xszq.nereides

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.call.*
import io.ktor.client.statement.*
import korlibs.io.util.UUID
import xyz.xszq.nereides.message.Face
import xyz.xszq.nereides.message.GuildAt
import xyz.xszq.nereides.message.MessageChain
import xyz.xszq.nereides.message.PlainText
import xyz.xszq.nereides.payload.event.GuildAtMessageCreate
import xyz.xszq.nereides.payload.message.MessageArk
import xyz.xszq.nereides.payload.message.MessageEmbed
import xyz.xszq.nereides.payload.message.MessageMarkdown
import xyz.xszq.nereides.payload.message.MessageReference
import xyz.xszq.nereides.payload.post.PostChannelMessage
import xyz.xszq.nereides.payload.user.GuildMember
import xyz.xszq.nereides.payload.user.GuildUser

interface GuildApi {
    val logger: KLogger
    suspend fun get(api: String): HttpResponse
    suspend fun post(api: String, payload: Any): HttpResponse
    suspend fun multipart(api: String, values: Map<String, String>, file: Pair<String, Pair<String, ByteArray>>): HttpResponse
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
    ): HttpResponse {
        val response = post("/channels/$channelId/messages", PostChannelMessage(
            content = content,
            image = image,
            embed = embed,
            ark = ark,
            markdown = markdown,
            eventId = eventId,
            msgId = replyMsgId,
            messageReference = referMsgId?.let {
                MessageReference(referMsgId, true)
            }
        ))
        logger.info { "[$channelId] <- $content" }
        return response
    }
    suspend fun sendChannelMessageByMultipart(
        channelId: String,
        fileImage: ByteArray,
        content: String? = null,
        eventId: String? = null,
        replyMsgId: String? = null,
        filename: String = UUID.randomUUID().toString()
    ): HttpResponse {
        val response = multipart("/channels/$channelId/messages", buildMap {
            content?.let {
                put("content", it)
            }
            eventId?.let {
                put("event_id", it)
            }
            replyMsgId?.let {
                put("msg_id", replyMsgId)
            }
        }, Pair("file_image", Pair(filename, fileImage)))
        logger.info { "[$channelId] <- ${content?:""} [image:$filename]" }
        return response
    }
    suspend fun getBotInfo() = get("/users/@me").body<GuildUser>()
    suspend fun getMember(guildId: String, userId: String) = get("/guilds/$guildId/members/$userId").body<GuildMember>()
    suspend fun parseContent(data: GuildAtMessageCreate): MessageChain {
        val result = MessageChain()

        var str = data.content.trim()
        val regex = buildMap {
            put("emoji", Regex("<emoji:(\\d+)>"))
            put("at", Regex("<@!?(\\d+)>"))
        }
        while (true) {
            regex.map { (type, r) ->
                Pair(type, r.find(str))
            }.filter { it.second != null }.sortedBy { it.second ?.range ?.first }.firstOrNull() ?.let { (type, r) ->
                if (r!!.range.first != 0) {
                    result += PlainText(str.substring(0 until r.range.first).trim())
                }
                val message = when (type) {
                    "emoji" -> Face(1, r.groupValues[1])
                    "at" -> {
                        val id = r.groupValues[1]
                        val member = getMember(data.guildId, id)
                        GuildAt(member, member.user!!)
                    }
                    else -> return@let
                }
                result += message
                str = str.substring(r.range.last + 1 until str.length).trim()
            } ?: break
        }
        if (str.isNotEmpty())
            result += PlainText(str)

        return result
    }
}